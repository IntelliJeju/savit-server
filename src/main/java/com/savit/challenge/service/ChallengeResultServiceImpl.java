package com.savit.challenge.service;

import com.savit.challenge.dto.ChallengeResultDTO;
import com.savit.challenge.mapper.ChallengeResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChallengeResultServiceImpl implements ChallengeResultService {

    private final ChallengeResultMapper mapper;

    @Override
    public ChallengeResultDTO getMyChallengeResult(Long userId, Long challengeId) {
        ChallengeResultDTO dto = mapper.findMyChallengeResult(userId, challengeId);
        if (dto == null) {
            throw new IllegalArgumentException("참여 이력이 없습니다. challengeId=" + challengeId);
        }

        // 타입별 불필요 필드 null 처리(프론트 혼란 방지)
        if ("COUNT".equals(dto.getType())) {
            dto.setGoalAmount(null);
            dto.setActualAmount(null);
        } else if ("AMOUNT".equals(dto.getType())) {
            dto.setGoalCount(null);
            dto.setActualCount(null);
        }
        return dto;
    }
}