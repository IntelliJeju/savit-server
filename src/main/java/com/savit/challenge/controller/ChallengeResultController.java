package com.savit.challenge.controller;

import com.savit.challenge.dto.ChallengeResultDTO;
import com.savit.challenge.service.ChallengeResultService;
import com.savit.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/challenge")
@RequiredArgsConstructor
@Slf4j
public class ChallengeResultController {

    private final ChallengeResultService resultService;
    private final JwtUtil jwtUtil;

    @GetMapping("/{challengeId}/my-result")
    public ResponseEntity<ChallengeResultDTO> getMyResult(
            @PathVariable Long challengeId,
            HttpServletRequest request
    ) {
        Long userId = jwtUtil.getUserIdFromToken(request);
        ChallengeResultDTO dto = resultService.getMyChallengeResult(userId, challengeId);
        return ResponseEntity.ok(dto);
    }
}