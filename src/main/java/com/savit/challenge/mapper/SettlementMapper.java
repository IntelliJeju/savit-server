package com.savit.challenge.mapper;

import com.savit.challenge.domain.ChallengeParticipationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SettlementMapper {

    List<ChallengeParticipationVO> selectForUpdate(@Param("challengeId") Long challengeId);

    boolean existsAnyPrizeWritten(@Param("challengeId") Long challengeId);

    int updateWinnerPrize(@Param("challengeId") Long challengeId,
                          @Param("userId") Long userId,
                          @Param("payout") long payout);

    int updateLoserPrize(@Param("challengeId") Long challengeId,
                         @Param("userId") Long userId,
                         @Param("amount") long amount);

    int upsertPoint(@Param("userId") Long userId,
                    @Param("delta") long delta);

    Long findUserIdByEmail(@Param("email") String email);
}
