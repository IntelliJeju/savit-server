package com.savit.security;

import com.savit.common.exception.JwtTokenException;
import com.savit.user.domain.User;
import com.savit.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    /**
     * HttpServletRequest에서 userId 추출 (자동 토큰 갱신 포함)
     */
    public TokenResult getUserIdFromTokenWithAutoRefresh(HttpServletRequest request) {
        try {
            String accessToken = extractAccessTokenFromRequest(request);
            String refreshToken = extractRefreshTokenFromRequest(request);

            if (accessToken == null) {
                throw new JwtTokenException("Access Token이 없습니다.");
            }

            // 1. Access Token 검증
            JwtTokenProvider.TokenValidationResult validationResult =
                    jwtTokenProvider.validateTokenWithDetails(accessToken);
// 2. Access Token이 유효한 경우
            if (validationResult.isValid()) {
                // Access Token 타입 확인
                if (!jwtTokenProvider.isAccessToken(accessToken)) {
                    throw new JwtTokenException("Access Token이 아닙니다.");
                }

                // 사용자 ID 추출
                String userIdStr = jwtTokenProvider.getUserId(accessToken);
                if (userIdStr == null || userIdStr.isBlank()) {
                    throw new JwtTokenException("토큰에서 사용자 ID를 추출할 수 없습니다.");
                }

                try {
                    Long userId = Long.parseLong(userIdStr);
                    return TokenResult.builder()
                            .userId(userId)
                            .newAccessToken(null) // 갱신되지 않음
                            .tokenRefreshed(false)
                            .build();
                } catch (NumberFormatException e) {
                    throw new JwtTokenException("토큰의 사용자 ID 형식이 잘못되었습니다.");
                }
            }

            // 3. Access Token이 만료된 경우 - 자동 갱신 시도
            if (validationResult.isExpired()) {
                log.info("Access Token 만료 감지, 자동 갱신 시도");

                if (refreshToken == null) {
                    throw new JwtTokenException("Refresh Token이 없어 토큰 갱신이 불가능합니다.");
                }

                try {
                    // 만료된 Access Token에서 userId 추출
                    String userIdStr = jwtTokenProvider.getUserId(accessToken);
                    Long userId = Long.parseLong(userIdStr);

                    // Refresh Token으로 새 Access Token 발급
                    String newAccessToken = refreshAccessToken(userId, refreshToken);

                    log.info("토큰 자동 갱신 성공 - userId: {}", userId);

                    return TokenResult.builder()
                            .userId(userId)
                            .newAccessToken(newAccessToken)
                            .tokenRefreshed(true)
                            .build();

                } catch (Exception e) {
                    log.error("토큰 자동 갱신 실패", e);
                    throw new JwtTokenException("토큰 갱신에 실패했습니다: " + e.getMessage());
                }
            }

            // 4. Access Token이 유효하지 않은 경우 (변조 등)
            throw new JwtTokenException(validationResult.getMessage());

        } catch (JwtTokenException e) {
            // JwtTokenException은 그대로 던짐
            throw e;
        } catch (Exception e) {
            // 예상하지 못한 모든 예외를 JwtTokenException으로 변환 (500 에러 방지)
            log.error("토큰 처리 중 예상치 못한 오류 발생", e);
            throw new JwtTokenException("토큰 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * Refresh Token으로 새 Access Token 발급
     */
    private String refreshAccessToken(Long userId, String refreshToken) {
        try {
            // 1. Refresh Token 유효성 검증
            JwtTokenProvider.TokenValidationResult refreshValidation =
                    jwtTokenProvider.validateTokenWithDetails(refreshToken);

            if (!refreshValidation.isValid()) {
                throw new JwtTokenException("유효하지 않은 Refresh Token입니다: " + refreshValidation.getMessage());
            }

            // 2. Refresh Token 타입 확인
            if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
                throw new JwtTokenException("Refresh Token이 아닙니다.");
            }

            // 3. 사용자 조회 및 DB의 Refresh Token과 비교
            User user = userService.findById(userId);
            if (user == null) {
                throw new JwtTokenException("존재하지 않는 사용자입니다.");
            }

            if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
                throw new JwtTokenException("저장된 Refresh Token과 일치하지 않습니다.");
            }

            // 4. 새 Access Token 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(userId.toString());

            log.info("새 Access Token 발급 완료 - userId: {}", userId);
            return newAccessToken;

        } catch (JwtTokenException e) {
            // JwtTokenException은 그대로 던짐
            throw e;
        } catch (Exception e) {
            // 예상하지 못한 모든 예외를 JwtTokenException으로 변환 (500 에러 방지)
            log.error("토큰 갱신 중 예상치 못한 오류 발생", e);
            throw new JwtTokenException("토큰 갱신 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * Request Header에서 Access Token 추출
     */
    private String extractAccessTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Request Header에서 Refresh Token 추출
     */
    private String extractRefreshTokenFromRequest(HttpServletRequest request) {
        return request.getHeader("Refresh-Token");
    }

    /**
     * 기존 메서드 - 자동 갱신 기능 포함 (기존 코드 호환성 완벽 유지)
     */
    public Long getUserIdFromToken(HttpServletRequest request) {
        TokenResult result = getUserIdFromTokenWithAutoRefresh(request);

        // 토큰이 갱신된 경우 응답에 포함시키기 위해 request attribute에 저장
        if (result.isTokenRefreshed()) {
            request.setAttribute("newAccessToken", result.getNewAccessToken());
            request.setAttribute("tokenRefreshed", true);
        }

        return result.getUserId();
    }

    public Long getUserIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new JwtTokenException("토큰이 없습니다.");
        }

        JwtTokenProvider.TokenValidationResult validationResult =
                jwtTokenProvider.validateTokenWithDetails(token);

        if (!validationResult.isValid()) {
            throw new JwtTokenException(validationResult.getMessage());
        }

        String userIdStr = jwtTokenProvider.getUserId(token);
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new JwtTokenException("토큰에서 사용자 ID를 추출할 수 없습니다.");
        }

        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new JwtTokenException("토큰의 사용자 ID 형식이 잘못되었습니다.");
        }
    }

    public Long getUserIdFromExpiredToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new JwtTokenException("토큰이 없습니다.");
        }

        try {
            String userIdStr = jwtTokenProvider.getUserId(token);
            if (userIdStr == null || userIdStr.isBlank()) {
                throw new JwtTokenException("토큰에서 사용자 ID를 추출할 수 없습니다.");
            }
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new JwtTokenException("토큰의 사용자 ID 형식이 잘못되었습니다.");
        }
    }

    public boolean isValidToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    public boolean isTokenExpired(String token) {
        return jwtTokenProvider.isTokenExpired(token);
    }

    /**
     * 토큰 처리 결과를 담는 클래스
     */
    @lombok.Builder
    @lombok.Getter
    public static class TokenResult {
        private Long userId;
        private String newAccessToken;  // 갱신된 새 Access Token (갱신되지 않았으면 null)
        private boolean tokenRefreshed; // 토큰이 갱신되었는지 여부
    }
}
