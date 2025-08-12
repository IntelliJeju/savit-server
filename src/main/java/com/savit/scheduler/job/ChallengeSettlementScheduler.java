package com.savit.scheduler.job;

import com.savit.challenge.domain.ChallengeVO;
import com.savit.challenge.service.ChallengeCompletionService;
import com.savit.challenge.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeSettlementScheduler {

    private final ChallengeCompletionService completionService;
    private final SettlementService settlementService;

    // 성공자 처리(00:00 KST) 이후 10분 뒤 정산만 수행
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    public void runSettlementOnly() {
        LocalDate todayKst = LocalDate.now(ZoneId.of("Asia/Seoul"));
        log.info("===== [정산] 시작 — 기준일(KST): {} / 실행시각: 00:10", todayKst);

        List<ChallengeVO> ending = completionService.findChallengesEndingToday();
        if (ending == null || ending.isEmpty()) {
            log.info("===== [정산] 오늘 종료되는 챌린지가 없습니다. 작업을 종료합니다.");
            return;
        }

        int settled = 0;
        for (ChallengeVO ch : ending) {
            try {
                settlementService.settle(ch.getId());
                settled++;
            } catch (Exception e) {
                log.error("===== [정산] 챌린지 정산 실패 — challengeId={}", ch.getId(), e);
                // 다음 챌린지 계속 진행
            }
        }
        log.info("===== [정산] 완료 — 정산된 챌린지 수: {}", settled);
    }
}