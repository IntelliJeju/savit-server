package com.savit.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIRequestDTO {

    /**
     * 사용할 모델 : gpt 4o mini
     */
    private String model;
    
    /**
     * 대화 메시지 리스트
     */
    private List<Message> messages;
    
    /**
     * 최대 생성할 토큰 수
     * 500 으로 고정, 향후 다른 분석이 필요하면 늘리기
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    /**
     * 창의성 수준 (0.0-1.0)
     * 0.7로 고정해서 사용(application.properties)
     */
    private Double temperature;
    
    /**
     * 응답 형식 지정 (Structured Outputs 용)
     * JSON Schema를 통해 구조화된 응답을 받을 때 사용
     */
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;


    /**
     * 단일 사용자 메시지로 요청 생성 (기존 방식)
     */
    public static OpenAIRequestDTO createSingleMessage(String model, String prompt, Integer maxTokens, Double temperature) {
        Message userMessage = Message.builder()
                .role("user")
                .content(prompt)
                .build();
        
        return OpenAIRequestDTO.builder()
                .model(model)
                .messages(List.of(userMessage))
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();
    }
    
    /**
     * System Role + User Role 메시지로 요청 생성 (Structured Outputs 방식)
     */
    public static OpenAIRequestDTO createSystemUserMessage(String model, String systemPrompt, String userPrompt, 
                                                            Integer maxTokens, Double temperature, ResponseFormat responseFormat) {
        Message systemMessage = Message.system(systemPrompt);
        Message userMessage = Message.user(userPrompt);
        
        return OpenAIRequestDTO.builder()
                .model(model)
                .messages(List.of(systemMessage, userMessage))
                .maxTokens(maxTokens)
                .temperature(temperature)
                .responseFormat(responseFormat)
                .build();
    }
    
    /**
     * 잔소리 메시지 생성용 Structured Outputs 요청 생성
     */
    public static OpenAIRequestDTO createNaggingMessageRequest(String model, Integer maxTokens, Double temperature) {
        String systemPrompt =
                """
                    You are a creative copywriter who crafts witty, slightly sarcastic, and playful short messages.
                    Your target audience is young professionals in their 20s and 30s, especially those in the first 0–3 years of their career.
                    You should sound like a concerned but humorous friend.
                    Include emojis and Korean-style reactions(you can use Korean-slang).
                    Each message must be one sentence long. Do not output a numbered list.
                    Answer me in Korean.
                """;
        
        String userPrompt =
                """
                    Generate 5 alert messages that can be sent when someone is using their credit card too frequently.
                    The messages should discourage overspending and encourage budgeting.
                    Answer me in Korean.
                """;
        
        return createSystemUserMessage(model, systemPrompt, userPrompt, maxTokens, temperature, 
                                       ResponseFormat.createNaggingMessageFormat());
    }
    
    /**
     * 하루 마무리 메시지 생성용 Structured Outputs 요청 생성
     */
    public static OpenAIRequestDTO createWrapUpMessageRequest(String model, Integer maxTokens, Double temperature) {
        String systemPrompt =
                """
                    You are a warm, supportive, and motivational friend who helps young professionals in their 20s and 30s improve their financial habits.
                    You speak in Korean, using emojis to make your messages feel personal and encouraging.
                    Each message should be one sentence long, focusing on reflection and tomorrow's goals.
                    Your style should celebrate small wins, give gentle reminders about financial goals, and inspire positive action for the next day.
                """;
        
        String userPrompt =
                """
                    Generate 5 end-of-day messages for young professionals who are working on managing their finances and spending habits.
                    These messages should be sent at 9 PM to help them reflect on today’s spending and encourage better financial habits tomorrow.
                    Topics can include reflecting on today’s spending, preparing for tomorrow’s budget, celebrating small financial wins, or gentle reminders about financial goals.
                    Do not output a numbered list. Answer me in Korean.
                """;
        
        return createSystemUserMessage(model, systemPrompt, userPrompt, maxTokens, temperature, 
                                       ResponseFormat.createWrapUpMessageFormat());
    }
}