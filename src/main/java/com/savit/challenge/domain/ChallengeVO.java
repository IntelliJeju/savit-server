package com.savit.challenge.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ChallengeVO {
    private Long id;
    private String title;
    private String description;
    private Date startDate;
    private Date endDate;
    private int targetCount;
    private BigDecimal targetAmount;
    private String type;
    private int durationWeeks;
    private Long categoryId;
    private Long totalParticipants;
    private Long joinedParticipants;
}