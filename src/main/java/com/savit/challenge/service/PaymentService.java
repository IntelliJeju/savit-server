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
        if (chal.getStartDate() != null && now.before(chal.getStartDate()))
            throw new IllegalStateException("챌린지가 아직 시작되지 않았습니다.");
        if (chal.getEndDate() != null && now.after(chal.getEndDate()))
            throw new IllegalStateException("이미 종료된 챌린지입니다.");

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

    // ✅ 하나만 남기기
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

    /** 웹훅/verify 공용 멱등 처리 */
    @Transactional
    public void verifyAndConfirm(Long requesterUserIdOrNull, String merchantUidOrNull, String impUidOrNull) {
        // 1) Iamport 단건 조회 (impUid 우선)
        IamportPaymentDTO pay = iamportService.fetchPayment(impUidOrNull, merchantUidOrNull);
        final String merchantUid = pay.getMerchantUid();
        if (merchantUid == null || merchantUid.isBlank()) {
            throw new IllegalStateException("아임포트 응답에 merchant_uid 없음");
        }

        // 2) 행 잠금
        PaymentVO p = paymentMapper.findByMerchantUidForUpdate(merchantUid);
        if (p == null) throw new IllegalStateException("Unknown merchantUid: " + merchantUid);

        // 3) 요청자 소유 검증
        if (requesterUserIdOrNull != null && !Objects.equals(p.getUserId(), requesterUserIdOrNull)) {
            throw new IllegalStateException("주문 소유자 불일치");
        }

        // 4) 상태 검증
        if (!"paid".equalsIgnoreCase(pay.getStatus())) {
            throw new IllegalStateException("아임포트 상태 불일치: " + pay.getStatus());
        }

        // 5) 금액 검증 (원화 정수)
        if (pay.getAmount() == null) throw new IllegalStateException("아임포트 응답 금액 없음");
        long payAmount = pay.getAmount().setScale(0, RoundingMode.UNNECESSARY).longValueExact();

        Long dbAmountObj = (p.getAmount() instanceof Long) ? (Long) p.getAmount() : null; // 만약 VO가 Object 반환 형태라면
        // ↑ 만약 VO 메서드 시그니처가 long이라면 위 한 줄 대신: long dbAmount = p.getAmount();

        if (dbAmountObj == null) { // Long인 경우 null 가드
            throw new IllegalStateException("DB 금액이 null 입니다.");
        }
        long dbAmount = dbAmountObj.longValue();

        if (payAmount != dbAmount) {
            throw new IllegalStateException("금액 불일치: iamport=" + payAmount + ", db=" + dbAmount);
        }

        // 6) 멱등 가드
        String curr = p.getStatus();
        if ("SUCCESS".equals(curr)) return;
        if (!"PENDING".equals(curr)) return;

        // 7) 결제 성공 마킹
        Long paidAtSecLong = pay.getPaidAt();
        final long paidAtSec = (paidAtSecLong != null ? paidAtSecLong : 0L);
        final Date paidAt = paidAtSec > 0 ? new Date(paidAtSec * 1000L) : new Date();
        paymentMapper.markSuccess(merchantUid, pay.getImpUid(), paidAt);

        // 8) 챌린지 참여 (멱등)
        boolean alreadyJoined = challengeParticipationMapper
                .existsParticipation(p.getChallengeId(), p.getUserId());
        if (!alreadyJoined) {
            // point와 동일하게 원 단위 long 사용
            challengeParticipationMapper.insertParticipation(
                    p.getChallengeId(),
                    p.getUserId(),
                    BigDecimal.valueOf(dbAmount) // 컬럼이 DECIMAL이면 BigDecimal로 맞춰 저장
            );
            log.info("챌린지 참여 등록 완료: challengeId={}, userId={}", p.getChallengeId(), p.getUserId());
        }

        // 9) 카드 트랜잭션 저장
        insertCardTransactionFromIamport(pay, p.getUserId());

        // 10) 포인트 적립
        pointMapper.ensureRow(p.getUserId());
        pointMapper.add(p.getUserId(), dbAmount);
    }

    private void insertCardTransactionFromIamport(IamportPaymentDTO pay, Long userId) {
        var card = cardMapper.findFirstCardByUserId(userId);
        if (card == null) {
            log.warn("카드 트랜잭션 저장 실패: userId={}의 카드 정보 없음", userId);
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
        tx.setResCancelYn("N"); // ✅ 일관성
        tx.setResCancelAmount("");
        tx.setResTotalAmount("");
        tx.setBudgetCategoryId(null);
        tx.setCategoryId(null);
        tx.setResMemberStoreName(pay.getName());
        tx.setResMemberStoreType(pay.getPgProvider());
        tx.setCreatedAt(new Date());
        tx.setUpdatedAt(new Date());

        cardTransactionMapper.insert(tx);
        log.info("카드 트랜잭션 저장 완료: {}", tx);
    }
}