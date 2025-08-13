package com.savit.challenge.service;

import com.savit.challenge.domain.ChallengeParticipationVO;
import com.savit.challenge.mapper.SettlementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final SettlementMapper mapper;
    private static final String OPS_EMAIL = "ops@savit.local";

    @Override
    @Transactional
    public void settle(Long challengeId) {
        log.info("[정산] 시작 - challengeId={}", challengeId);

        // 멱등 가드: 이미 prize가 기록된 적 있으면 스킵
        if (mapper.existsAnyPrizeWritten(challengeId)) {
            log.warn("[정산] 이미 정산 완료되어 스킵 - challengeId={}", challengeId);
            return;
        }

        // 정산 대상 잠금 (FOR UPDATE)
        List<ChallengeParticipationVO> parts = mapper.selectForUpdate(challengeId);
        if (parts == null || parts.isEmpty()) {
            log.info("[정산] 참가자가 없어 종료 - challengeId={}", challengeId);
            return;
        }

        // 분류: 성공자 / 실패자
        var winners = parts.stream().filter(p -> "SUCCESS".equals(p.getStatus())).toList();
        var losers  = parts.stream().filter(p -> "FAIL".equals(p.getStatus())).toList();

        // 실패자: 수수료(1%) 및 풀 계산 (원 단위 정수, 소수점 버림)
        long feeSum = 0L; // 회사 수수료 합계
        long pool   = 0L; // 성공자에게 배분할 풀(= 실패자 예치금 - 수수료)
        for (var l : losers) {
            long d   = nz(l.getMyFee()); // 예치금
            long fee = fee1pct(d);
            feeSum += fee;
            pool   += (d - fee);
        }

        // 성공자 배분: 풀을 성공자 예치금 비율대로 분배 (정수 나눗셈 → 버림)
        long wSum = winners.stream().mapToLong(w -> nz(w.getMyFee())).sum();
        long paid = 0L;
        for (var w : winners) {
            long s = nz(w.getMyFee());
            long payout = (wSum == 0L) ? 0L : (pool * s) / wSum;
            if (payout != 0L) {
                mapper.updateWinnerPrize(challengeId, w.getUserId(), payout);
                mapper.upsertPoint(w.getUserId(), payout);
                paid += payout;
            }
        }

        // 실패자 회수: 예치금만큼 prize에 음수 반영 및 포인트 차감
        for (var l : losers) {
            long d = nz(l.getMyFee());
            if (d != 0L) {
                mapper.updateLoserPrize(challengeId, l.getUserId(), d);
                mapper.upsertPoint(l.getUserId(), -d);
            }
        }

        // 운영자 적립: 수수료 합 + 분배 후 남는 잔액(버림으로 생김)
        long remainder = pool - paid;
        Long opsUserId = mapper.findUserIdByEmail(OPS_EMAIL);
        if (opsUserId == null) {
            throw new IllegalStateException("운영 계정을 찾을 수 없습니다: " + OPS_EMAIL);
        }

        long opsGain = feeSum + remainder;
        if (opsGain != 0L) {
            mapper.upsertPoint(opsUserId, opsGain);
        }

        log.info(
                "[정산] 완료 - cid={}, 성공자 수={}, 실패자 수={}, 수수료합={}, 배분풀={}, 지급합계={}, 잔여금={}, 운영자적립={}",
                challengeId, winners.size(), losers.size(), feeSum, pool, paid, remainder, opsGain
        );
    }

    private long nz(Long v) { return (v == null) ? 0L : v; }
    private long fee1pct(long myFee) { return myFee / 100; }
}

