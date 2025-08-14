package com.savit.challenge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ChallengeResultDTO {
    private Long challengeId;
    private String title;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    private String type;
    private BigDecimal goalCount;
    private BigDecimal goalAmount;
    private String myStatus;
    private Long myDeposit;
    private Long actualCount;
    private BigDecimal actualAmount;
    private Long totalParticipants;
    private Long survivorCount;
    private Long totalDeposit;
    private Long forfeitedDeposit;
    private Boolean ended;
}