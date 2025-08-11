package com.savit.challenge.mapper;

import com.savit.challenge.dto.ChallengeUpdateRequestDTO;
import com.savit.challenge.dto.ChallengeSummaryDTO;
import com.savit.challenge.dto.ParticipantInfo;
import com.savit.challenge.dto.ParticipationStatusDTO;
import com.savit.challenge.dto.ChallengeFailedParticipantDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ChallengeParticipationMapper {
    boolean existsParticipation(@Param("challengeId") Long challengeId,
                                @Param("userId") Long userId);

    void insertParticipation(@Param("challengeId") Long challengeId,
                             @Param("userId") Long userId,
                             @Param("myFee") BigDecimal myFee);

    // entry_fee 제거에 따라 challengeParticipation 테이블에서 my_fee 합치기
    BigDecimal sumMyFeeByChallenge(Long challengeId);
  
    List<ParticipantInfo> selectParticipantsWithAmount(Long challengeId);
    List<ParticipantInfo> selectParticipantsWithCount(Long challengeId);

    // 특정 카테고리의 진행중인 챌린지 참여자들 조회
    List<ParticipationStatusDTO> findActiveParticipantsByCategory(Long categoryId);

    // 특정 챌린지의 진행중인 참여자들 조회( 종료일 체크용)
    List<ParticipationStatusDTO> findParticipatingUsersByChallengeId(Long challengeId);

    // 챌린지 참여자 진행상황 업데이트
    void updateChallengeProgress(@Param("participationId") Long participationId, @Param("count") Long count, @Param("amount") BigDecimal amount);

    // 챌린지 상태 Fail 로
    void updateStatusToFail(ChallengeUpdateRequestDTO request);

    // 챌린지 상태 success 로 일괄 변경
    void updateStatusToSuccess(@Param("participationIds") List<Long> participationIds, @Param("completedAt") String completedAt);

    // 특정 참여자의 현재 진행상황 조회
    ParticipationStatusDTO findParticipationById (Long participationId);
  
    List<ChallengeSummaryDTO>  selectChallengeSummary(Long userId);
    
    // 새로 실패한 참여자 목록 조회 (알림 발송용)
    List<ChallengeFailedParticipantDTO> findNewlyFailedParticipants(@Param("targetDate") String targetDate);
}
