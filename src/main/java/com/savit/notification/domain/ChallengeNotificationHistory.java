package com.savit.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 챌린지 알림 발송 이력 관리
 * 중복 알림 방지를 위한 테이블
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeNotificationHistory {
    
    private Long id;
    private Long userId;
    private Long challengeId;
    private String notificationType; // SUCCESS, FAIL 로 지정되게끔
    private String challengeTitle;
    private LocalDateTime sentAt;
    private String targetDate; // YYYYMMDD 형식 (같은 날 중복 방지용)
    
    /**
     * 중복 체크용 키 생성
     */
    public String getUniqueKey() {
        return String.format("%d_%d_%s_%s", userId, challengeId, notificationType, targetDate);
    }
    
    /**
     * 성공 알림 생성
     */
    public static ChallengeNotificationHistory createSuccess(Long userId, Long challengeId, String challengeTitle, String targetDate) {
        return ChallengeNotificationHistory.builder()
                .userId(userId)
                .challengeId(challengeId)
                .notificationType("SUCCESS")
                .challengeTitle(challengeTitle)
                .targetDate(targetDate)
                .sentAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 실패 알림 생성
     */
    public static ChallengeNotificationHistory createFail(Long userId, Long challengeId, String challengeTitle, String targetDate) {
        return ChallengeNotificationHistory.builder()
                .userId(userId)
                .challengeId(challengeId)
                .notificationType("FAIL")
                .challengeTitle(challengeTitle)
                .targetDate(targetDate)
                .sentAt(LocalDateTime.now())
                .build();
    }
}