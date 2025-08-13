package com.savit.user.dto;

import com.savit.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoDTO {
    private String email;
    private String nickname;
    private String profileImage;
    private String kakaoUserId;

    // User 객체로부터 DTO 생성
    public static UserInfoDTO from(User user) {
        return UserInfoDTO.builder()
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .kakaoUserId(user.getKakaoUserId())
                .build();
    }
}
