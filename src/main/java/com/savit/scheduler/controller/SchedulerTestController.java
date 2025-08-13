package com.savit.scheduler.controller;

import com.savit.budget.service.BudgetMonitoringService;
import com.savit.card.dto.BudgetMonitoringDTO;
import com.savit.card.service.AsyncCardApprovalService;
import com.savit.card.service.CardApprovalService;
import com.savit.notification.service.NotificationService;
import com.savit.scheduler.job.*;
import com.savit.user.mapper.UserMapper;
import com.savit.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 스케줄러 테스트용 컨트롤러
 * 실제 스케줄러 동작을 수동으로 테스트하기 위한 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/test/scheduler")
@RequiredArgsConstructor
public class SchedulerTestController {

    private final CardApprovalScheduler cardApprovalScheduler;
    private final RandomNaggingScheduler randomNaggingScheduler;
    private final AsyncCardApprovalService asyncCardApprovalService;
    private final BudgetMonitoringService budgetMonitoringService;
    private final CardApprovalService cardApprovalService;
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    private final ChallengeDropoutScheduler challengeDropoutScheduler;
    private final DailyTopSpendingScheduler dailyTopSpendingScheduler;
    private final ChallengeStartNotificationScheduler challengeStartNotificationScheduler;


    /**
     * 모든 사용자 카드 승인내역 동기화 스케줄러 수동 실행
     */
    @PostMapping("/card-sync")
    public ResponseEntity<Map<String, Object>> testCardSyncScheduler() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== 카드 승인내역 동기화 스케줄러 수동 테스트 시작 ===");

            // 스케줄러 수동 실행
            cardApprovalScheduler.syncAllUsersCardApprovals();

            response.put("status", "success");
            response.put("message", "카드 승인내역 동기화 스케줄러 실행 완료");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("카드 승인내역 동기화 스케줄러 테스트 실패", e);
            response.put("status", "error");
            response.put("message", "실행 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 사용자 카드 승인내역 동기화 테스트
     */
    @PostMapping("/card-sync/user/{userId}")
    public ResponseEntity<Map<String, Object>> testUserCardSync(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== 사용자 {} 카드 승인내역 동기화 테스트 시작 ===", userId);

            // 비동기 서비스로 특정 사용자 처리
            asyncCardApprovalService.processUserCardApprovalsAsync(userId);

            response.put("status", "success");
            response.put("message", "사용자 " + userId + " 카드 동기화 실행 완료");
            response.put("userId", userId);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("사용자 {} 카드 동기화 테스트 실패", userId, e);
            response.put("status", "error");
            response.put("message", "실행 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 사용자 예산 모니터링 테스트
     */
    @PostMapping("/budget-check/user/{userId}")
    public ResponseEntity<Map<String, Object>> testUserBudgetCheck(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== 사용자 {} 예산 모니터링 테스트 시작 ===", userId);

            // 예산 모니터링 수동 실행
            budgetMonitoringService.checkBudgetAndSendNotifications(userId);

            // 예산 데이터 조회해서 응답에 포함
            BudgetMonitoringDTO budgetData =
                    cardApprovalService.getBudgetMonitoringData(userId);

            response.put("status", "success");
            response.put("message", "사용자 " + userId + " 예산 모니터링 완료");
            response.put("userId", userId);
            response.put("hasBudget", budgetData.isHasBudget());
            response.put("isOverBudget", budgetData.isOverBudget());
            response.put("isWarningLevel", budgetData.isWarningLevel());
            response.put("usageRate", budgetData.getUsageRate());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("사용자 {} 예산 모니터링 테스트 실패", userId, e);
            response.put("status", "error");
            response.put("message", "실행 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 랜덤 잔소리 스케줄러 수동 실행
     */
    @PostMapping("/random-nagging")
    public ResponseEntity<Map<String, Object>> testRandomNaggingScheduler() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== 랜덤 잔소리 스케줄러 수동 테스트 시작 ===");

            // 스케줄러 수동 실행 (07~ 다음날 01시까지 30분간격으로 날리는거임 실제 실행 X)
//            randomNaggingScheduler.sendRandomNaggingNotifications();
            // 스케줄러 수동 실행 (지금 시간부터 3분간 매 1분마다 발송 테스트용)
            randomNaggingScheduler.sendRandomNaggingNotificationsForTest();

            response.put("status", "success");
            response.put("message", "랜덤 잔소리 스케줄러 실행 완료");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("랜덤 잔소리 스케줄러 테스트 실패", e);
            response.put("status", "error");
            response.put("message", "실행 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * OpenAI 기반 하루 마무리 알림 스케줄러 수동 실행
     */
    @PostMapping("/daily-wrapup")
    public ResponseEntity<Map<String, Object>> testDailyWrapUpScheduler() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== OpenAI 기반 하루 마무리 알림 스케줄러 수동 테스트 시작 ===");

            // 테스트용 스케줄러 수동 실행
            randomNaggingScheduler.sendDailyWrapUpNotificationsForTest();

            response.put("status", "success");
            response.put("message", "OpenAI 기반 하루 마무리 알림 스케줄러 실행 완료");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("하루 마무리 알림 스케줄러 테스트 실패", e);
            response.put("status", "error");
            response.put("message", "실행 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 스케줄러 대상 사용자 목록 조회
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getSchedulerTargetUsers() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 카드 등록된 사용자
            List<User> usersWithCards = userMapper.findUsersWithCards();
            List<Long> cardUserIds = usersWithCards.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());

            // FCM 토큰 등록된 사용자
            List<User> usersWithTokens = userMapper.findUsersWithFcmTokens();
            List<Long> tokenUserIds = usersWithTokens.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());

            response.put("status", "success");
            response.put("usersWithCards", cardUserIds);
            response.put("usersWithFcmTokens", tokenUserIds);
            response.put("cardUsersCount", cardUserIds.size());
            response.put("tokenUsersCount", tokenUserIds.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("스케줄러 대상 사용자 조회 실패", e);
            response.put("status", "error");
            response.put("message", "조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 스케줄러 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            response.put("status", "active");
            response.put("message", "스케줄러가 정상 동작 중입니다");
            response.put("schedulers", Map.of(
                    "cardApprovalScheduler", "08:00, 12:00, 18:00, 00:00 실행",
                    "randomNaggingScheduler", "매 30분마다 실행 (07:00-01:00)",
                    "healthCheckScheduler", "매시간 정각 실행"
            ));
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "상태 확인 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 챌린지 일일 낙오 요약 알림 테스트
     */
    @PostMapping("/challenge-dropout-summary")
    public ResponseEntity<Map<String, Object>> testChallengeDropoutSummary() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== 챌린지 일일 낙오 요약 알림 수동 테스트 시작 ===");

            challengeDropoutScheduler.sendDailyDropoutSummary();

            response.put("status", "success");
            response.put("message", "챌린지 일일 낙오 요약 알림 발송 완료");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("챌린지 일일 낙오 요약 알림 테스트 실패", e);
            response.put("status", "error");
            response.put("message", "실행 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 일일 최고 지출 데이터 수집 테스트
     */
    @PostMapping("/daily-top-spending/collect")
    public ResponseEntity<Map<String, Object>> testCollectDailyTopSpending() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== 일일 최고 지출 데이터 수집 수동 테스트 시작 ===");

            dailyTopSpendingScheduler.collectDailyTopSpending();

            response.put("status", "success");
            response.put("message", "일일 최고 지출 데이터 수집 완료");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("일일 최고 지출 데이터 수집 테스트 실패", e);
            response.put("status", "error");
            response.put("message", "실행 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 일일 최고 지출 알림 발송 테스트
     */
    @PostMapping("/daily-top-spending/notify")
    public ResponseEntity<Map<String, Object>> testSendDailyTopSpendingNotifications() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== 일일 최고 지출 알림 발송 수동 테스트 시작 ===");

            dailyTopSpendingScheduler.sendDailyTopSpendingNotifications();

            response.put("status", "success");
            response.put("message", "일일 최고 지출 알림 발송 완료");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("일일 최고 지출 알림 발송 테스트 실패", e);
            response.put("status", "error");
            response.put("message", "실행 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 내일 시작하는 챌린지 알림 테스트
     */
    @PostMapping("/challenge-start/tomorrow")
    public ResponseEntity<Map<String, Object>> testTomorrowChallengeStartNotifications() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== 내일 시작 챌린지 알림 수동 테스트 시작 ===");

            challengeStartNotificationScheduler.sendTomorrowChallengeStartNotifications();

            response.put("status", "success");
            response.put("message", "내일 시작 챌린지 알림 발송 완료");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("내일 시작 챌린지 알림 테스트 실패", e);
            response.put("status", "error");
            response.put("message", "실행 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 오늘 시작하는 챌린지 알림 테스트
     */
    @PostMapping("/challenge-start/today")
    public ResponseEntity<Map<String, Object>> testTodayChallengeStartNotifications() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== 오늘 시작 챌린지 알림 수동 테스트 시작 ===");

            challengeStartNotificationScheduler.sendTodayChallengeStartNotifications();

            response.put("status", "success");
            response.put("message", "오늘 시작 챌린지 알림 발송 완료");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("오늘 시작 챌린지 알림 테스트 실패", e);
            response.put("status", "error");
            response.put("message", "실행 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }


}