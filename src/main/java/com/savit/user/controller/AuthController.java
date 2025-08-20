package com.savit.user.controller;

import com.savit.user.service.AuthService;
import com.savit.user.dto.LoginResponseDTO;
import com.savit.security.KakaoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final KakaoProperties kakaoProperties;

    // 카카오 로그인 시작
    @GetMapping("/kakao")
    public String kakaoLogin() {
        try {
            String kakaoAuthUrl = "https://kauth.kakao.com/oauth/authorize" +
                    "?response_type=code" +
                    "&client_id=" + kakaoProperties.getClientId() +
                    "&redirect_uri=" + URLEncoder.encode(kakaoProperties.getRedirectUri(), "UTF-8");

            log.info("카카오 인증 URL로 리다이렉트: {}", kakaoAuthUrl);
            return "redirect:" + kakaoAuthUrl;
        } catch (Exception e) {
            log.error("카카오 인증 URL 생성 실패", e);
            return "redirect:/login?error=true";
        }
    }

    @GetMapping("/login/kakao")
    public void kakaoCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {

        log.info("카카오 콜백 호출됨. code={}, error={}", code, error);

        // 에러 처리
        if (error != null) {
            log.error("카카오 인증 에러: {}", error);
            String errorUrl = "https://savit.cloud/auth/error?message=" +
                    URLEncoder.encode("카카오 인증이 취소되었습니다.", "UTF-8");
            response.sendRedirect(errorUrl);
            return;
        }

        // code 파라미터 체크
        if (code == null || code.trim().isEmpty()) {
            log.error("인가 코드가 없습니다.");
            String errorUrl = "https://savit.cloud/auth/error?message=" +
                    URLEncoder.encode("인가 코드가 필요합니다.", "UTF-8");
            response.sendRedirect(errorUrl);
            return;
        }

        try {
            log.info("======= 카카오 로그인 처리 시작 ========");

            // 카카오 로그인 처리
            LoginResponseDTO loginResponse = authService.kakaoLogin(code);

            log.info("======== 카카오 로그인 처리 완료 ==========");
            log.info("accessToken = {}", loginResponse.getAccessToken());
            log.info("refreshToken = {}", loginResponse.getRefreshToken());

            // 성공 시 프론트엔드로 리다이렉트 (토큰 포함)
            String redirectUrl = "https://savit.cloud/auth/login/callback" +
                    "?accessToken=" + URLEncoder.encode(loginResponse.getAccessToken(), "UTF-8") +
                    "&refreshToken=" + URLEncoder.encode(loginResponse.getRefreshToken(), "UTF-8");

            response.sendRedirect(redirectUrl);

            log.info("카카오 로그인 성공. 사용자: {}", loginResponse.getUser().getEmail());

        } catch (Exception e) {
            log.error("카카오 로그인 처리 실패", e);
            try {
                String errorUrl = "https://savit.cloud/auth/error?message=" +
                        URLEncoder.encode("로그인 처리 중 오류가 발생했습니다.", "UTF-8");
                response.sendRedirect(errorUrl);
            } catch (IOException ioException) {
                log.error("에러 페이지 리다이렉트 실패", ioException);
            }
        }
    }


    /**
     * 로그아웃 (토큰 무효화)
     */
    @PostMapping("/logout")
    @ResponseBody
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            // 실제로는 토큰을 블랙리스트에 추가하거나 DB에서 무효화 처리
            // 현재는 단순히 성공 응답만 반환
            log.info("로그아웃 처리 완료");
            return ResponseEntity.ok("로그아웃되었습니다.");
        } catch (Exception e) {
            log.error("로그아웃 처리 실패:", e);
            return ResponseEntity.internalServerError().body("로그아웃 처리 중 오류가 발생했습니다.");
        }
    }

    // Refresh Token 요청 DTO
    @lombok.Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }
}