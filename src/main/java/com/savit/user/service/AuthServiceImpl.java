package com.savit.user.service;

import com.savit.security.JwtTokenProvider;
import com.savit.security.TokenPairDTO;
import com.savit.user.domain.User;
import com.savit.user.dto.KakaoTokenDTO;
import com.savit.user.dto.KakaoUserDTO;
import com.savit.user.dto.LoginResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final KakaoOAuthService kakaoOAuthService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponseDTO kakaoLogin(String code) {
        try {
            log.info("1. 인가 코드로 액세스 토큰 요청 시작");

            // 1. 인가 코드로 액세스 토큰 요청
            KakaoTokenDTO tokenDto = kakaoOAuthService.getAccessToken(code);
            log.info("1. 액세스 토큰 요청 완료: {}", tokenDto.getAccessToken());

            log.info("2. 액세스 토큰으로 사용자 정보 요청 시작");

            // 2. 액세스 토큰으로 사용자 정보 요청
            KakaoUserDTO kakaoUserDto = kakaoOAuthService.getUserInfo(tokenDto.getAccessToken());
            log.info("2. 사용자 정보 요청 완료: {}", kakaoUserDto.getId());

            log.info("3. 사용자 정보 추출 시작");

            // 3. 사용자 정보 추출
            String email = kakaoUserDto.getKakaoAccount().getEmail();
            String nickname = kakaoUserDto.getKakaoAccount().getProfile().getNickname();
            String profileImage = kakaoUserDto.getKakaoAccount().getProfile().getProfileImageUrl();
            String kakaoUserId = kakaoUserDto.getId().toString();

            log.info("3. 사용자 정보 추출 완료 - email: {}, nickname: {}", email, nickname);

            // 4. 기존 사용자 조회 또는 신규 사용자 생성
            User user = userService.findByEmail(email);
            if (user == null) {
                user = User.builder()
                        .email(email)
                        .nickname(nickname)
                        .profileImage(profileImage)
                        .kakaoUserId(kakaoUserId)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                userService.createUser(user);
            } else {
                // 기존 사용자 정보 업데이트
                user.setNickname(nickname);
                user.setProfileImage(profileImage);
                user.setKakaoUserId(kakaoUserId);
                user.setUpdatedAt(LocalDateTime.now());
                userService.updateUser(user);
            }

            // 5. Access Token + Refresh Token 생성 (userId 기반)
            TokenPairDTO tokenPair = jwtTokenProvider.createTokenPair(user.getId().toString());

            // 6. Refresh Token을 DB에 저장
            user.setRefreshToken(tokenPair.getRefreshToken());
            userService.updateUser(user);

            log.info("JWT 토큰 발급 완료 - userId: {}", user.getId());

            return LoginResponseDTO.builder()
                    .accessToken(tokenPair.getAccessToken())
                    .refreshToken(tokenPair.getRefreshToken())
                    .user(User.builder()
                            .id(user.getId())
                            .email(user.getEmail())
                            .nickname(user.getNickname())
                            .profileImage(user.getProfileImage())
                            .kakaoUserId(user.getKakaoUserId())
                            .createdAt(user.getCreatedAt())
                            .updatedAt(user.getUpdatedAt())
                            .build())
                    .accessTokenExpiresIn(600L)  // 10분
                    .refreshTokenExpiresIn(604800L)  // 7일
                    .success(true)
                    .message("로그인 성공")
                    .build();
        } catch (Exception e) {
            log.error("카카오 로그인 처리 중 오류 발생:", e);
            throw e;
        }
    }


}
