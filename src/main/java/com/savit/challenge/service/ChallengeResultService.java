package com.savit.challenge.service;

import com.savit.challenge.dto.ChallengeResultDTO;

public interface ChallengeResultService {
    ChallengeResultDTO getMyChallengeResult(Long userId, Long challengeId);
}
