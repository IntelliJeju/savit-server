package com.savit.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI Structured Outputs - 잔소리 메시지 응답 DTO
 * JSON Schema를 통해 구조화된 응답을 받기 위한 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaggingMessageResponseDTO {
    
    /**
     * 생성된 잔소리 메시지 리스트
     * 10개의 다양한 잔소리 메시지
     */
    private List<String> messages;
    
    /**
     * 메시지 생성 날짜 (YYYY-MM-DD 형식)
     */
    private String generatedDate;
    
    /**
     * 메시지 타입 (항상 "nagging")
     */
    private String type;
    
    /**
     * 메시지 톤 (예: "friendly", "strict", "humorous")
     */
    private String tone;
}