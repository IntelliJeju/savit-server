package com.savit.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI Structured Outputs - 하루 마무리 메시지 응답 DTO
 * JSON Schema를 통해 구조화된 응답을 받기 위한 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WrapUpMessageResponseDTO {
    
    /**
     * 생성된 하루 마무리 메시지 리스트
     * 5개의 다양한 마무리 메시지
     */
    private List<String> messages;
}