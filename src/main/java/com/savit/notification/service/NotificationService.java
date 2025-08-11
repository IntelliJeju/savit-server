package com.savit.notification.service;

import com.google.firebase.messaging.*;
import com.savit.openai.service.OpenAIInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.savit.notification.dto.PushNotificationRequest;
import com.savit.notification.domain.PushNotification;
import com.savit.notification.domain.UserFcmToken;
import com.savit.notification.domain.ChallengeNotificationHistory;
import com.savit.notification.mapper.NotificationMapper;
import com.savit.notification.mapper.ChallengeNotificationHistoryMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final NotificationMapper notificationMapper;
    private final OpenAIInternalService openAIInternalService;
    private final ChallengeNotificationHistoryMapper challengeNotificationHistoryMapper;

    public String sendNotification(PushNotificationRequest request) {
        return sendNotification(request, null);
    }

    public String sendNotification(PushNotificationRequest request, Long userId) {
        if (firebaseMessaging == null) {
            log.error("FirebaseMessaging is not initialized");
            throw new RuntimeException("Firebase messaging service is not available");
        }

        Notification notification = Notification.builder()
                .setTitle(request.getTitle())
                .setBody(request.getBody())
                .setImage(request.getImage())
                .build();

        Message message = Message.builder()
                .setToken(request.getToken())
                .setNotification(notification)
                .putAllData(request.getData() != null ? request.getData() : java.util.Collections.emptyMap())
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(WebpushNotification.builder()
                                .setTitle(request.getTitle())
                                .setBody(request.getBody())
                                .setIcon("/icon-192x192.png")
                                .build())
                        .build())
                .build();

        try {
            String response = firebaseMessaging.send(message);
            log.info("Successfully sent message: {}", response);

            PushNotification pushNotification = PushNotification.builder()
                    .userId(userId)
                    .fcmToken(request.getToken())
                    .title(request.getTitle())
                    .body(request.getBody())
                    .status("SENT")
                    .sentAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationMapper.insertNotification(pushNotification);

            return response;
        } catch (FirebaseMessagingException e) { // 에러 확인용으로 DB에 일단 저장
            log.error("Failed to send message: {}", e.getMessage());

            PushNotification pushNotification = PushNotification.builder()
                    .userId(userId)
                    .fcmToken(request.getToken())
                    .title(request.getTitle())
                    .body(request.getBody())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationMapper.insertNotification(pushNotification);

            throw new RuntimeException("Failed to send notification", e);
        }
    }

    public void saveUserFcmToken(Long userId, String fcmToken, String deviceType) {
        notificationMapper.deactivateUserTokens(userId, deviceType);
        notificationMapper.insertUserFcmToken(userId, fcmToken, deviceType);
        log.info("FCM token saved for user: {} with device type: {}", userId, deviceType);
    }

    public List<UserFcmToken> getActiveTokensByUserId(Long userId) {
        return notificationMapper.findActiveTokensByUserId(userId);
    }

    public void sendNotificationToUser(Long userId, String title, String body) {
        List<UserFcmToken> tokens = getActiveTokensByUserId(userId);

        for (UserFcmToken token : tokens) {
            PushNotificationRequest request = new PushNotificationRequest(
                    token.getFcmToken(), title, body
            );
            try {
                sendNotification(request, userId);
            } catch (Exception e) {
                log.error("Failed to send notification to token: {}", token.getFcmToken(), e);
            }
        }
    }
    
    // 비즈니스 로직 기반 알림 전송 메서드들
    
    /**
     * 예산 초과 알림
     */
    public void sendBudgetExceededNotification(Long userId, String exceededAmount, String totalBudget) {
        String title = "💸 예산 초과 알림";
        String body = String.format("이번 달 예산을 %s 초과했어요! (예산: %s)", exceededAmount, totalBudget);
        sendNotificationToUser(userId, title, body);
        log.info("예산 초과 알림 전송 완료 - 사용자: {}, 초과금액: {}", userId, exceededAmount);
    }
    
    /**
     * 카테고리별 예산 경고 (80% 사용시)
     */
    public void sendCategoryBudgetWarning(Long userId, String categoryName, int usagePercent, String remainingAmount) {
        String title = "⚠️ 예산 사용 경고";
        String body = String.format("%s 예산의 %d%% 사용! %s만 남았어요!", categoryName, usagePercent, remainingAmount);
        sendNotificationToUser(userId, title, body);
        log.info("카테고리 예산 경고 전송 완료 - 사용자: {}, 카테고리: {}, 사용률: {}%", userId, categoryName, usagePercent);
    }
    
    /**
     * 카드 사용 알림
     */
    public void sendCardUsageNotification(Long userId, String storeName, String amount, String storeType) {
        String title = "💳 카드 사용 알림";
        String body = String.format("%s에서 %s 사용 (%s)", 
                storeName != null ? storeName : "가맹점", 
                amount, 
                storeType != null ? storeType : "");
        sendNotificationToUser(userId, title, body);
        log.info("카드 사용 알림 전송 완료 - 사용자: {}, 금액: {}, 가맹점: {}", userId, amount, storeName);
    }
    
    /**
     * 랜덤 잔소리 알림
     */
    public void sendRandomNaggingNotification(Long userId) {
        String naggingMessage = getRandomNaggingMessage();
        String title = "💬 Savit 한마디";
        sendNotificationToUser(userId, title, naggingMessage);
        log.info("랜덤 잔소리 알림 전송 완료 - 사용자: {}, 메시지: {}", userId, naggingMessage);

    }

    /**
     * GPT 프롬프팅 응답 결과로 나온 잔소리 메세지
     * 또는 디폴트 메세지 전송
     * @return aiMessage or defaultMessage
     */
    private String getRandomNaggingMessage() {
        try {
            if(openAIInternalService.isServiceEnabled() && !openAIInternalService.getDailyAnswers().isEmpty()) {
                String aiResponse = openAIInternalService.getDailyAnswers().get(0);
                String[] aiMessages = aiResponse.split("\\n");  // 정규표현식에서 개행 문자 찾는 용도로 \\n 사용함
                
                // 유효한 메시지만 필터링
                List<String> validMessages = new ArrayList<>();
                for (String message : aiMessages) {
                    String trimmed = message.trim();
                    // 빈 문자열이 아니고, 10자 이상이고, 한글이 포함된 메시지만 선택
                    if (!trimmed.isEmpty() && trimmed.length() > 10 && trimmed.matches(".*[가-힣].*")) {
                        validMessages.add(trimmed);
                    }
                }
                
                if (!validMessages.isEmpty()) {
                    String selectedMessage = validMessages.get((int) (Math.random() * validMessages.size()));
                    log.debug("선택된 AI 메시지: {}", selectedMessage);
                    return selectedMessage;
                } else {
                    log.warn("유효한 AI 메시지가 없음, 기본 메시지 사용");
                }
            }
        } catch (Exception e) {
            log.error("GPT 응답 메세지 사용 실패, 기본 메세지를 사용합니다", e);
        }
        String[] defaultMessages = {
            "또 신용카드 긁기만 해봐 💸",
            "돈 관리 좀 제대로 해보자! 💰",
            "잔여 예산 확인은 언제 할 거야? 📊",
            "용돈 기입장이라도 써봐! 📝",
            "카드 명세서 보면 깜짝 놀랄걸? 😱",
            "절약 좀 해보자구요~",
            "이번 달 예산 벌써 다 썼어? 😤",
            "신용카드 또 긁었어? 아니지?"
        };
        return defaultMessages[(int) (Math.random() * defaultMessages.length)];
    }
    
    /**
     * 챌린지 성공 알림 - 실제 운영용
     * 중복 알림 방지 로직 포함
     */
    public void sendChallengeSuccessNotification(Long userId, Long challengeId, String challengeTitle, String prize) {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            // 중복 알림 체크
            if (challengeNotificationHistoryMapper.existsByUserChallengeTypeDate(userId, challengeId, "SUCCESS", today)) {
                log.debug("사용자 {} 챌린지 {} 성공 알림 이미 발송됨 - 중복 발송 방지", userId, challengeTitle);
                return;
            }
            
            String title = "🎉 챌린지 성공!";
            String body = String.format("'%s' 챌린지를 성공했어요! 상금: %s", challengeTitle, prize);
            sendNotificationToUser(userId, title, body);
            
            // 알림 발송 이력 저장
            ChallengeNotificationHistory history = ChallengeNotificationHistory.createSuccess(userId, challengeId, challengeTitle, today);
            challengeNotificationHistoryMapper.insertNotificationHistory(history);
            
            log.info("챌린지 성공 알림 전송 완료 - 사용자: {}, 챌린지: {}, 상금: {}", userId, challengeTitle, prize);
            
        } catch (Exception e) {
            log.error("챌린지 성공 알림 전송 실패 - 사용자: {}, 챌린지: {}", userId, challengeTitle, e);
        }
    }
    
    /**
     * 챌린지 실패 알림 - 실제 운영용
     * 중복 알림 방지 로직 포함
     */
    public void sendChallengeFailNotification(Long userId, Long challengeId, String challengeTitle) {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            // 중복 알림 체크
            if (challengeNotificationHistoryMapper.existsByUserChallengeTypeDate(userId, challengeId, "FAIL", today)) {
                log.debug("사용자 {} 챌린지 {} 실패 알림 이미 발송됨 - 중복 발송 방지", userId, challengeTitle);
                return;
            }
            
            String title = "😢 챌린지 실패";
            String body = String.format("'%s' 챌린지에 실패했어요. 다음에 더 열심히 해봐요!", challengeTitle);
            sendNotificationToUser(userId, title, body);
            
            // 알림 발송 이력 저장
            ChallengeNotificationHistory history = ChallengeNotificationHistory.createFail(userId, challengeId, challengeTitle, today);
            challengeNotificationHistoryMapper.insertNotificationHistory(history);
            
            log.info("챌린지 실패 알림 전송 완료 - 사용자: {}, 챌린지: {}", userId, challengeTitle);
            
        } catch (Exception e) {
            log.error("챌린지 실패 알림 전송 실패 - 사용자: {}, 챌린지: {}", userId, challengeTitle, e);
        }
    }
    
    /**
     * 챌린지 성공 알림 - 테스트용 (하위 호환성)
     */
    public void sendChallengeSuccessNotification(Long userId, String challengeTitle, String prize) {
        // 테스트용은 challengeId를 -1로 설정하여 중복 체크 우회
        sendChallengeSuccessNotification(userId, -1L, challengeTitle, prize);
    }
    
    /**
     * 챌린지 실패 알림 - 테스트용 (하위 호환성)
     */
    public void sendChallengeFailNotification(Long userId, String challengeTitle) {
        // 테스트용은 challengeId를 -1로 설정하여 중복 체크 우회
        sendChallengeFailNotification(userId, -1L, challengeTitle);
    }
}