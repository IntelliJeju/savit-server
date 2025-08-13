package com.savit.challenge.mapper;

import com.savit.challenge.dto.ChallengeResultDTO;
import org.apache.ibatis.annotations.Param;

public interface ChallengeResultMapper {
    ChallengeResultDTO findMyChallengeResult(@Param("userId") Long userId,
                                             @Param("challengeId") Long challengeId);
}
