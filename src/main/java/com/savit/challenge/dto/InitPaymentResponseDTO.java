package com.savit.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Builder
public class InitPaymentResponseDTO {
    private final String merchantUid;
    private final BigDecimal amount;
}