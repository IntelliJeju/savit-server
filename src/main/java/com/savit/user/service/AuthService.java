package com.savit.user.service;

import com.savit.user.dto.LoginResponseDTO;

public interface AuthService {

    /**
     * 카카오 로그인 처리
     * @param code 카카오에서 받은 Authorization Code
     * @return 로그인 응답 (JWT 토큰 + 사용자 정보)
     */
    LoginResponseDTO kakaoLogin(String code);


}