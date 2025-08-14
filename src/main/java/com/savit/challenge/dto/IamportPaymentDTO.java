package com.savit.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IamportPaymentDTO {
    private String status;
    private String merchantUid;
    private String impUid;
    private BigDecimal amount;
    private long paidAt;
    private Long userId;
    private Long challengeId;
    private String name;
    private String pgProvider;
}
