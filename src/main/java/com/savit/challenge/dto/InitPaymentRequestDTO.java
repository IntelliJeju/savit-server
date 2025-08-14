package com.savit.challenge.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class InitPaymentRequestDTO {
    private Long challengeId;
    private BigDecimal desiredAmount;
}