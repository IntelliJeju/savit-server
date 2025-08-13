package com.savit.card.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString(exclude = {"cardPassword", "encryptedCardNo"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    private Long id;
    private String connectedId;
    private String organization;
    private String cardName;
    private String issuer;
    private String encryptedCardNo;
    private String resCardNo;
    private String cardPassword;
    private String resCardType;
    private String resSleepYn;
    private String resImageLink;
    private LocalDateTime registeredAt;
    private LocalDateTime updatedAt;
    private Long userId;
}
