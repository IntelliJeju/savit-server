package com.savit.card.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
public class CardRegisterRequestDTO {
    @NotBlank(message = "기관코드는 필수입니다.")
    private String organization;
    @NotBlank(message = "로그인 ID는 필수입니다.")
    private String loginId;
    @NotBlank(message = "로그인 비밀번호는 필수입니다.")
    private String loginPw;
    @NotBlank(message = "생년월일은 필수입니다.")
    private String birthDate;
    @NotBlank(message = "카드 비밀번호는 필수입니다.")
    private String cardPassword;
    @NotBlank(message = "카드번호는 필수입니다.")
    private String encryptedCardNo;
}
