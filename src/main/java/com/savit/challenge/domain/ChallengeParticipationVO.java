package com.savit.challenge.domain;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ChallengeParticipationVO {
    private Long id;
    private String status;
    private Long challengeId;
    private Long userId;
    private Date createdAt;
    private Date completedAt;
    private Date updatedAt;
    private Long challengeCount;
    private BigDecimal challengeAmount;
    private Long myFee;
    private Long prize;
}
