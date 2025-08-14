package com.savit.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI Structured Outputs - Response Format DTO
 * 요청시 response_format 필드에 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseFormat {
    
    /**
     * 응답 형식 타입 (Structured Outputs의 경우 "json_schema")
     */
    private String type;
    
    /**
     * JSON Schema 정의
     */
    @JsonProperty("json_schema")
    private JsonSchema jsonSchema;
    
    /**
     * 잔소리 메시지용 Response Format 생성
     */
    public static ResponseFormat createNaggingMessageFormat() {
        return ResponseFormat.builder()
                .type("json_schema")
                .jsonSchema(createNaggingMessageSchema())
                .build();
    }
    
    /**
     * 하루 마무리 메시지용 Response Format 생성
     */
    public static ResponseFormat createWrapUpMessageFormat() {
        return ResponseFormat.builder()
                .type("json_schema")
                .jsonSchema(createWrapUpMessageSchema())
                .build();
    }
    
    /**
     * 잔소리 메시지 JSON Schema 생성
     */
    private static JsonSchema createNaggingMessageSchema() {
        JsonSchema.Schema schema = JsonSchema.Schema.builder()
                .type("object")
                .properties(java.util.Map.of(
                    "messages", java.util.Map.of(
                        "type", "array",
                        "items", java.util.Map.of("type", "string"),
                        "minItems", 10,
                        "maxItems", 10,
                        "description", "10개의 다양한 잔소리 메시지"
                    ),
                    "generatedDate", java.util.Map.of(
                        "type", "string",
                        "pattern", "^\\d{4}-\\d{2}-\\d{2}$",
                        "description", "메시지 생성 날짜 (YYYY-MM-DD 형식)"
                    ),
                    "type", java.util.Map.of(
                        "type", "string",
                        "enum", java.util.List.of("nagging"),
                        "description", "메시지 타입"
                    ),
                    "tone", java.util.Map.of(
                        "type", "string",
                        "enum", java.util.List.of("friendly", "strict", "humorous"),
                        "description", "메시지 톤"
                    )
                ))
                .required(new String[]{"messages", "generatedDate", "type", "tone"})
                .additionalProperties(false)
                .build();
        
        return JsonSchema.builder()
                .name("nagging_message_response")
                .strict(true)
                .schema(schema)
                .build();
    }
    
    /**
     * 하루 마무리 메시지 JSON Schema 생성
     */
    private static JsonSchema createWrapUpMessageSchema() {
        JsonSchema.Schema schema = JsonSchema.Schema.builder()
                .type("object")
                .properties(java.util.Map.of(
                    "messages", java.util.Map.of(
                        "type", "array",
                        "items", java.util.Map.of("type", "string"),
                        "minItems", 10,
                        "maxItems", 10,
                        "description", "10개의 다양한 하루 마무리 메시지"
                    ),
                    "generatedDate", java.util.Map.of(
                        "type", "string",
                        "pattern", "^\\d{4}-\\d{2}-\\d{2}$",
                        "description", "메시지 생성 날짜 (YYYY-MM-DD 형식)"
                    ),
                    "type", java.util.Map.of(
                        "type", "string",
                        "enum", java.util.List.of("daily_wrap_up"),
                        "description", "메시지 타입"
                    ),
                    "tone", java.util.Map.of(
                        "type", "string",
                        "enum", java.util.List.of("encouraging", "reflective", "motivational"),
                        "description", "메시지 톤"
                    )
                ))
                .required(new String[]{"messages", "generatedDate", "type", "tone"})
                .additionalProperties(false)
                .build();
        
        return JsonSchema.builder()
                .name("daily_wrap_up_response")
                .strict(true)
                .schema(schema)
                .build();
    }
}