package com.savit.challenge.domain;

import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter @Setter
public class PaymentVO {
    private String merchantUid;
    private Long amount;
    private String status;
    private String impUid;
    private Date paidAt;
    private Date createdAt;
    private Long challengeId;
    private Long userId;
}