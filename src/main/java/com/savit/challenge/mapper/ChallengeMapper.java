package com.savit.challenge.mapper;

import com.savit.challenge.domain.ChallengeVO;
import com.savit.challenge.dto.ChallengeListDTO;
import com.savit.challenge.dto.ChallengeStatusDTO;
import com.savit.challenge.dto.ChallengeDropoutSummaryDTO;
import com.savit.notification.dto.ChallengeNotificationDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ChallengeMapper {

    // 시작일 기준
    List<ChallengeNotificationDTO> findByStartDate(@Param("startDate") LocalDate startDate);

    // 종료일 기준
    List<ChallengeNotificationDTO> findByEndDate(@Param("endDate") LocalDate endDate);

    // 챌린지 ID로 참여 중인 유저 ID 리스트 조회
    List<Long> findUserIdsByChallengeId(@Param("challengeId") Long challengeId);

    // 리스트 조회
    List<Long> findSuccessfulWeeklyCategories(Long userId);

    List<ChallengeListDTO> findWeeklyChallenges();

    List<ChallengeListDTO> findMonthlyChallenges(Long categoryid);


    // 참여중인 챌린지 조회
    List<ChallengeListDTO> findParticipatingChallenges(Long userId);

    // 챌린지 타입 찾기
    String selectChallengeType(Long challengeId);

    //챌린지 현황 조회
    ChallengeStatusDTO selectChallengeStatus(@Param("challengeId") Long challengeId, @Param("userId") Long userId);

    // 진행 중인 챌린지별 낙오 요약 통계 조회
    List<ChallengeDropoutSummaryDTO> getChallengeDropoutSummaries();

    // 특정 챌린지의 FCM 토큰 등록된 참여자 조회
    List<Long> findParticipantUserIdsByChallengeId(@Param("challengeId") Long challengeId);

    // 특정 날짜에 종료되는 챌린지 조회
    List<ChallengeVO> findChallengesEndingOnDate(String endDate);

    // 상세조회
    ChallengeVO findById(Long challengeId);

}
