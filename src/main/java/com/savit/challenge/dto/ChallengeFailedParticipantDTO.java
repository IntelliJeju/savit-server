package com.savit.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 챌린지 실패한 참여자 정보
 * 알림 발송을 위한 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeFailedParticipantDTO {
    
    private Long userId;
    private Long challengeId;
    private String challengeTitle;
    private String status; // FAIL
    private String failReason; // 실패 사유 (선택적)
    
    /**
     * 실패한 참여자 생성
     */
    public static ChallengeFailedParticipantDTO createFailed(Long userId, Long challengeId, String challengeTitle, String failReason) {
        return ChallengeFailedParticipantDTO.builder()
                .userId(userId)
                .challengeId(challengeId)
                .challengeTitle(challengeTitle)
                .status("FAIL")
                .failReason(failReason)
                .build();
    }
}