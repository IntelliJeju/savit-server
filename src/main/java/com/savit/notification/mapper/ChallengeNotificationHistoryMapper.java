package com.savit.notification.mapper;

import com.savit.notification.domain.ChallengeNotificationHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 챌린지 알림 발송 이력 매퍼
 */
@Mapper
public interface ChallengeNotificationHistoryMapper {
    
    /**
     * 알림 발송 이력 저장
     */
    void insertNotificationHistory(ChallengeNotificationHistory history);
    
    /**
     * 중복 알림 체크 - 같은 날 같은 타입 알림 발송 여부 확인
     */
    boolean existsByUserChallengeTypeDate(
            @Param("userId") Long userId,
            @Param("challengeId") Long challengeId,
            @Param("notificationType") String notificationType,
            @Param("targetDate") String targetDate
    );
    
    /**
     * 사용자별 특정 챌린지 알림 이력 조회
     */
    List<ChallengeNotificationHistory> findByUserIdAndChallengeId(
            @Param("userId") Long userId,
            @Param("challengeId") Long challengeId
    );
    
    /**
     * 특정 날짜 알림 발송 이력 조회 (디버깅용)
     */
    List<ChallengeNotificationHistory> findByTargetDate(@Param("targetDate") String targetDate);
    
    /**
     * 오래된 알림 이력 정리 (30일 이상 된 데이터)
     */
    void deleteOldNotificationHistory(@Param("retentionDays") int retentionDays);
}