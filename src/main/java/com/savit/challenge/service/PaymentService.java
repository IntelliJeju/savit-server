package com.savit.challenge.service;

import com.savit.card.domain.CardTransactionVO;
import com.savit.card.mapper.CardMapper;
import com.savit.card.mapper.CardTransactionMapper;
import com.savit.challenge.domain.ChallengeVO;
import com.savit.challenge.domain.PaymentVO;
import com.savit.challenge.dto.IamportPaymentDTO;
import com.savit.challenge.dto.InitPaymentRequestDTO;
import com.savit.challenge.dto.InitPaymentResponseDTO;
import com.savit.challenge.dto.PaymentStatusResponseDTO;
import com.savit.challenge.mapper.ChallengeMapper;
import com.savit.challenge.mapper.ChallengeParticipationMapper;
import com.savit.challenge.mapper.PaymentMapper;
import com.savit.challenge.mapper.PointMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final PointMapper pointMapper;
    private final ChallengeMapper challengeMapper;
    private final ChallengeParticipationMapper challengeParticipationMapper;
    private final CardMapper cardMapper;
    private final CardTransactionMapper cardTransactionMapper;
    private final IamportService iamportService;

    @Transactional
    public InitPaymentResponseDTO initPayment(Long userId, InitPaymentRequestDTO req) {
        ChallengeVO chal = challengeMapper.findById(req.getChallengeId());
        if (chal == null) throw new IllegalArgumentException("존재하지 않는 챌린지입니다.");

        Date now = new Date();

        if (chal.getStartDate() != null && !now.before(chal.getStartDate())) {
            throw new IllegalStateException("이미 시작된 챌린지는 결제할 수 없습니다.");
        }
        if (chal.getEndDate() != null && now.after(chal.getEndDate())) {
            throw new IllegalStateException("이미 종료된 챌린지입니다.");
        }

        boolean already = challengeParticipationMapper.existsParticipation(req.getChallengeId(), userId);
        if (already) throw new IllegalStateException("이미 참여 중인 챌린지입니다.");

        BigDecimal amount = decideAmount(userId, req.getChallengeId(), req.getDesiredAmount());

        String merchantUid = "order-" + userId + "-" + System.currentTimeMillis();
        paymentMapper.insertPending(merchantUid, userId, req.getChallengeId(), amount.longValue());
        iamportService.prepare(merchantUid, amount);

        return InitPaymentResponseDTO.builder()
                .merchantUid(merchantUid)
                .amount(amount)
                .build();
    }

    private BigDecimal decideAmount(Long userId, Long challengeId, BigDecimal desired) {
        return desired != null ? desired : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponseDTO getStatus(String merchantUid) {
        PaymentVO p = paymentMapper.findByMerchantUid(merchantUid);
        String status = (p == null) ? "PENDING" : p.getStatus();
        return PaymentStatusResponseDTO.builder()
                .merchantUid(merchantUid)
                .status(status)
                .build();
    }

    /** 웹훅/verify 공용 멱등 처리 (환불 미적용, 멱등/락만) */
    @Transactional
    public void verifyAndConfirm(Long requesterUserIdOrNull, String merchantUidOrNull, String impUidOrNull) {
        // 1) 아임포트 결제 단건 조회 (impUid 우선)
        IamportPaymentDTO pay = iamportService.fetchPayment(impUidOrNull, merchantUidOrNull);
        final String merchantUid = pay.getMerchantUid();
        if (merchantUid == null || merchantUid.isBlank()) {
            throw new IllegalStateException("아임포트 응답에 merchant_uid 없음");
        }

        // 2) 결제 레코드 잠금 (멱등)
        PaymentVO p = paymentMapper.findByMerchantUidForUpdate(merchantUid);
        if (p == null) throw new IllegalStateException("알 수 없는 주문입니다. merchantUid=" + merchantUid);

        // 3) 요청자 소유 검증
        if (requesterUserIdOrNull != null && !Objects.equals(p.getUserId(), requesterUserIdOrNull)) {
            throw new IllegalStateException("주문 소유자가 일치하지 않습니다.");
        }

        // 4) 상태 검증
        if (!"paid".equalsIgnoreCase(pay.getStatus())) {
            throw new IllegalStateException("아임포트 상태가 'paid'가 아닙니다. status=" + pay.getStatus());
        }

        // 5) 금액 검증 (원화 정수)
        if (pay.getAmount() == null) throw new IllegalStateException("아임포트 응답 금액이 없습니다.");
        long payAmount = pay.getAmount().setScale(0, RoundingMode.UNNECESSARY).longValueExact();

        Long dbAmountObj = (p.getAmount() instanceof Long) ? (Long) p.getAmount() : null;
        if (dbAmountObj == null) throw new IllegalStateException("DB 금액이 null 입니다.");
        long dbAmount = dbAmountObj;

        if (payAmount != dbAmount) {
            throw new IllegalStateException("결제 금액 불일치: iamport=" + payAmount + ", db=" + dbAmount);
        }

        // 6) 멱등 가드
        String curr = p.getStatus();
        if ("SUCCESS".equals(curr)) {
            log.info("[결제 검증] 이미 SUCCESS 상태로 처리된 주문입니다. merchantUid={}", merchantUid);
            return;
        }
        if (!"PENDING".equals(curr)) {
            log.info("[결제 검증] PENDING이 아닌 상태라 처리하지 않습니다. merchantUid={}, status={}", merchantUid, curr);
            return;
        }

        // 7) 결제 성공 마킹
        Long paidAtSecLong = pay.getPaidAt();
        final long paidAtSec = (paidAtSecLong != null ? paidAtSecLong : 0L);
        final Date paidAt = paidAtSec > 0 ? new Date(paidAtSec * 1000L) : new Date();
        paymentMapper.markSuccess(merchantUid, pay.getImpUid(), paidAt);
        log.info("[결제 검증] 결제 성공 반영 완료. merchantUid={}, userId={}, challengeId={}",
                merchantUid, p.getUserId(), p.getChallengeId());

        // 8) 챌린지 참여 (멱등 + 락) — 환불 미적용
        boolean joined = tryJoinWithLocks(p.getUserId(), p.getChallengeId(), BigDecimal.valueOf(dbAmount));
        if (!joined) {
            // 환불은 미적용 상태
            log.warn("[참여 실패] 정원 마감 또는 경쟁 패배로 참여 확정 실패. 결제는 성공 상태입니다. merchantUid={}, userId={}, challengeId={}",
                    merchantUid, p.getUserId(), p.getChallengeId());
        } else {
            log.info("[참여 성공] 챌린지 참여 확정. userId={}, challengeId={}", p.getUserId(), p.getChallengeId());
        }

        // 9) 카드 트랜잭션 저장
        insertCardTransactionFromIamport(pay, p.getUserId());

        // 10) 포인트 적립 (일단 항상 적립 / 환불은 추후 추가하거나..)
        pointMapper.ensureRow(p.getUserId());
        pointMapper.add(p.getUserId(), dbAmount);
        log.info("[포인트] 적립 완료. userId={}, amount={}", p.getUserId(), dbAmount);
    }

    /** 낙관락(CAS) + 막판 비관락으로 안전하게 참여 인서트 (환불 없음 버전) */
    private boolean tryJoinWithLocks(Long userId, Long challengeId, BigDecimal myFee) {
        // 이미 참여했다면 멱등 종료
        if (challengeParticipationMapper.existsParticipation(challengeId, userId)) {
            log.info("[참여 멱등] 이미 참여 중입니다. userId={}, challengeId={}", userId, challengeId);
            return true;
        }

        // 무제한이면 바로 인서트 시도
        ChallengeVO ch = challengeMapper.findChallengeForJoin(challengeId);
        if (ch == null) {
            log.warn("[참여 실패] 챌린지가 존재하지 않습니다. challengeId={}", challengeId);
            return false;
        }
        if (ch.getTotalParticipants() == null) {
            return insertParticipationIdempotent(challengeId, userId, myFee);
        }

        // 1) 낙관락(CAS) 1~3회
        for (int i = 0; i < 3; i++) {
            int ok = challengeMapper.tryReserveSeatCAS(challengeId, ch.getVersion());
            if (ok == 1) {
                return insertParticipationIdempotent(challengeId, userId, myFee);
            }
            // 실패 → 최신 버전 재조회 후 재시도
            ch = challengeMapper.findChallengeForJoin(challengeId);
            if (ch == null) {
                log.warn("[참여 실패] 챌린지를 재조회했으나 존재하지 않습니다. challengeId={}", challengeId);
                return false;
            }
            if (ch.getTotalParticipants() == null) {
                return insertParticipationIdempotent(challengeId, userId, myFee);
            }
        }

        // 2) 막판: 비관락(짧게 1회)
        ChallengeVO locked = challengeMapper.findForUpdate(challengeId); // 같은 @Transactional 안
        Long cap = locked.getTotalParticipants();
        if (cap == null) {
            return insertParticipationIdempotent(challengeId, userId, myFee);
        }
        long current = challengeParticipationMapper.countByChallengeId(challengeId);
        if (current >= cap) {
            log.info("[정원 초과] 현재 인원={} / 정원={}. userId={}, challengeId={}", current, cap, userId, challengeId);
            return false; // 정원 초과
        }
        return insertParticipationIdempotent(challengeId, userId, myFee);
    }

    /** UNIQUE 제약으로 멱등 보장 */
    private boolean insertParticipationIdempotent(Long challengeId, Long userId, BigDecimal myFee) {
        try {
            challengeParticipationMapper.insertParticipation(challengeId, userId, myFee);
            log.info("[참여 등록] 참여 인서트 완료. userId={}, challengeId={}, myFee={}", userId, challengeId, myFee);
            return true;
        } catch (DuplicateKeyException e) {
            // 이미 참여됨 → 멱등 성공으로 간주
            log.info("[참여 멱등] 이미 참여되어 있습니다. userId={}, challengeId={}", userId, challengeId);
            return true;
        }
    }

    private void insertCardTransactionFromIamport(IamportPaymentDTO pay, Long userId) {
        var card = cardMapper.findFirstCardByUserId(userId);
        if (card == null) {
            log.warn("[카드 트랜잭션] 사용자 카드 정보가 없어 저장하지 않습니다. userId={}", userId);
            return;
        }

        Date paidDate = new Date(pay.getPaidAt() * 1000L);
        var dateFormat = new java.text.SimpleDateFormat("yyyyMMdd");
        var timeFormat = new java.text.SimpleDateFormat("HHmmss");

        CardTransactionVO tx = new CardTransactionVO();
        tx.setCardId(card.getId());
        tx.setResCardNo(card.getResCardNo());
        tx.setResUsedDate(dateFormat.format(paidDate));
        tx.setResUsedTime(timeFormat.format(paidDate));
        tx.setResUsedAmount(String.valueOf(pay.getAmount().longValue()));
        tx.setResCancelYn("N");
        tx.setResCancelAmount("");
        tx.setResTotalAmount("");
        tx.setBudgetCategoryId(null);
        tx.setCategoryId(null);
        tx.setResMemberStoreName(pay.getName());
        tx.setResMemberStoreType(pay.getPgProvider());
        tx.setCreatedAt(new Date());
        tx.setUpdatedAt(new Date());

        cardTransactionMapper.insert(tx);
        log.info("[카드 트랜잭션] 저장 완료: {}", tx);
    }
}