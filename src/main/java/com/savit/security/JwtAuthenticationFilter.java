package com.savit.security;

import com.savit.common.exception.JwtTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements Filter {

    private final JwtUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if ("OPTIONS".equals(httpRequest.getMethod())) {
            httpResponse.setHeader("Access-Control-Allow-Origin", "*");
            httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, Refresh-Token");
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            chain.doFilter(request, response);
            return;
        }
        try {
            // 기존 메서드 사용 - 내부적으로 자동 갱신 처리됨
            Long userId = jwtUtil.getUserIdFromToken(httpRequest);

            // userId를 request attribute에 설정
            httpRequest.setAttribute("userId", userId.toString());

            // 토큰이 갱신되었는지 확인하여 응답 헤더에 포함
            String newAccessToken = (String) httpRequest.getAttribute("newAccessToken");
            Boolean tokenRefreshed = (Boolean) httpRequest.getAttribute("tokenRefreshed");

            if (tokenRefreshed != null && tokenRefreshed && newAccessToken != null) {
                httpResponse.setHeader("New-Access-Token", newAccessToken);
                httpResponse.setHeader("Token-Refreshed", "true");
                log.info("토큰 자동 갱신됨 - userId: {}", userId);
            }

        } catch (JwtTokenException e) {
            log.error("JWT 인증 실패: {}", e.getMessage());
            sendUnauthorizedResponse(httpResponse, e.getMessage());
            return;
        } catch (Exception e) {
            // 예상하지 못한 모든 예외를 401로 처리 (절대 500 에러 방지)
            log.error("JWT 필터에서 예상치 못한 오류 발생", e);
            sendUnauthorizedResponse(httpResponse, "인증 처리 중 오류가 발생했습니다.");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 401 에러 응답 전송
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, Refresh-Token");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Expose-Headers", "New-Access-Token, Token-Refreshed");

        response.getWriter().write("\"" + message + "\"");
    }
}