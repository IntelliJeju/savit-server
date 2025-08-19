package com.savit.challenge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ParticipantInfo {
    private String nickName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal challengeAmount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long challengeCount;

    private String status;

}
