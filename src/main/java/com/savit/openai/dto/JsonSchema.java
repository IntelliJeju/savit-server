package com.savit.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * OpenAI Structured Outputs - JSON Schema 정의 DTO
 * response_format에서 사용할 JSON Schema 구조
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonSchema {
    
    /**
     * 스키마 이름
     */
    private String name;
    
    /**
     * Structured Outputs 엄격 모드 (항상 true)
     */
    private Boolean strict;
    
    /**
     * 실제 JSON Schema 정의
     */
    private Schema schema;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Schema {
        
        /**
         * 스키마 타입 (항상 "object")
         */
        private String type;
        
        /**
         * 객체 속성 정의
         */
        private Map<String, Object> properties;
        
        /**
         * 필수 필드 목록
         */
        private String[] required;
        
        /**
         * 추가 속성 허용 여부 (Structured Outputs에서는 false)
         */
        @JsonProperty("additionalProperties")
        private Boolean additionalProperties;
    }
}