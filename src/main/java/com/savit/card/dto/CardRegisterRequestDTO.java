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
    private String loginId;
    private String loginPw;
    private String birthDate;
    private String cardPassword;
    private String encryptedCardNo;
    // ocr 인식할 카드 이미지
    private MultipartFile cardImage;
}
