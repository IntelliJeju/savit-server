package com.savit.openai.controller;

import com.savit.openai.service.OpenAIInternalService;
import com.savit.openai.scheduler.OpenAIDailyScheduler;
import com.savit.openai.dto.NaggingMessageResponseDTO;
import com.savit.openai.dto.WrapUpMessageResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 서비스 관리용 컨트롤러
 */
@RestController
@RequestMapping("/api/openai")
@RequiredArgsConstructor
@Slf4j
public class OpenAIController {
    
    private final OpenAIInternalService openAIInternalService;
    private final OpenAIDailyScheduler openAIDailyScheduler;
    
    /**
     * 저장된 일일 답변 조회
     */
    @GetMapping("/daily-answers")
    public ResponseEntity<Map<String, Object>> getDailyAnswers() {
        try {
            List<String> answers = openAIInternalService.getDailyAnswers();
            List<String> prompts = openAIInternalService.getPredefinedPrompts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCount", answers.size());
            response.put("prompts", prompts);
            response.put("answers", answers);
            response.put("serviceEnabled", openAIInternalService.isServiceEnabled());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("일일 답변 조회 중 오류 발생", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "답변 조회 중 오류가 발생했습니다.");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
    
    /**
     * 특정 인덱스 답변 조회용 (테스트용)
     */
    @GetMapping("/daily-answers/{index}")
    public ResponseEntity<Map<String, Object>> getDailyAnswer(@PathVariable int index) {
        try {
            List<String> answers = openAIInternalService.getDailyAnswers();
            List<String> prompts = openAIInternalService.getPredefinedPrompts();
            
            if (index < 0 || index >= answers.size()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "유효하지 않은 인덱스입니다.");
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("index", index);
            response.put("prompt", prompts.get(index));
            response.put("answer", answers.get(index));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("특정 답변 조회 중 오류 발생", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "답변 조회 중 오류가 발생했습니다.");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
    
    /**
     * 수동 답변 갱신 (테스트용)
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> manualRefresh() {
        try {
            if (!openAIInternalService.isServiceEnabled()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "OpenAI 서비스가 비활성화되어 있습니다.");
                
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("수동 답변 갱신 요청");
            
            // 백그라운드에서 실행하여 응답 지연 방지
            new Thread(() -> {
                try {
                    openAIDailyScheduler.manualRefresh();
                } catch (Exception e) {
                    log.error("백그라운드 답변 갱신 중 오류 발생", e);
                }
            }).start();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "답변 갱신이 백그라운드에서 시작되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("수동 답변 갱신 중 오류 발생", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "답변 갱신 중 오류가 발생했습니다.");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
    
    /**
     * 저장된 하루 마무리 답변 조회
     */
    @GetMapping("/daily-wrapup-answers")
    public ResponseEntity<Map<String, Object>> getDailyWrapUpAnswers() {
        try {
            List<String> answers = openAIInternalService.getDailyWrapUpAnswers();
            List<String> prompts = openAIInternalService.getDailyWrapUpPrompts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCount", answers.size());
            response.put("prompts", prompts);
            response.put("answers", answers);
            response.put("serviceEnabled", openAIInternalService.isServiceEnabled());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("하루 마무리 답변 조회 중 오류 발생", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "하루 마무리 답변 조회 중 오류가 발생했습니다.");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * OpenAI 서비스 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("serviceEnabled", openAIInternalService.isServiceEnabled());
        response.put("answerCount", openAIInternalService.getAnswerCount());
        response.put("wrapUpAnswerCount", openAIInternalService.getDailyWrapUpAnswers().size());
        response.put("promptCount", openAIInternalService.getPredefinedPrompts().size());
        response.put("wrapUpPromptCount", openAIInternalService.getDailyWrapUpPrompts().size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Structured Outputs 잔소리 메시지 테스트
     * GPT-4o-mini의 Structured Outputs 기능을 테스트하여 JSON Schema 기반의 구조화된 응답 확인
     */
    @PostMapping("/test/structured-nagging")
    public ResponseEntity<Map<String, Object>> testStructuredNaggingMessage() {
        try {
            if (!openAIInternalService.isServiceEnabled()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "OpenAI 서비스가 비활성화되어 있습니다.");
                
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("Structured Outputs 잔소리 메시지 테스트 요청");
            
            NaggingMessageResponseDTO structuredResponse = openAIInternalService.testStructuredNaggingMessage();
            
            Map<String, Object> response = new HashMap<>();
            if (structuredResponse != null) {
                response.put("success", true);
                response.put("message", "Structured Outputs 테스트 성공");
                response.put("structuredResponse", structuredResponse);
                response.put("messageCount", structuredResponse.getMessages() != null ? structuredResponse.getMessages().size() : 0);
                response.put("generatedDate", structuredResponse.getGeneratedDate());
                response.put("type", structuredResponse.getType());
                response.put("tone", structuredResponse.getTone());
                response.put("messages", structuredResponse.getMessages());
            } else {
                response.put("success", false);
                response.put("message", "Structured Outputs 테스트 실패 - null 응답");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Structured Outputs 잔소리 메시지 테스트 중 오류 발생", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "테스트 중 오류가 발생했습니다.");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
    
    /**
     * Structured Outputs 하루 마무리 메시지 테스트
     * GPT-4o-mini의 Structured Outputs 기능을 테스트하여 JSON Schema 기반의 구조화된 응답 확인
     */
    @PostMapping("/test/structured-wrapup")
    public ResponseEntity<Map<String, Object>> testStructuredWrapUpMessage() {
        try {
            if (!openAIInternalService.isServiceEnabled()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "OpenAI 서비스가 비활성화되어 있습니다.");
                
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("Structured Outputs 하루 마무리 메시지 테스트 요청");
            
            WrapUpMessageResponseDTO structuredResponse = openAIInternalService.testStructuredWrapUpMessage();
            
            Map<String, Object> response = new HashMap<>();
            if (structuredResponse != null) {
                response.put("success", true);
                response.put("message", "Structured Outputs 테스트 성공");
                response.put("structuredResponse", structuredResponse);
                response.put("messageCount", structuredResponse.getMessages() != null ? structuredResponse.getMessages().size() : 0);
                response.put("generatedDate", structuredResponse.getGeneratedDate());
                response.put("type", structuredResponse.getType());
                response.put("tone", structuredResponse.getTone());
                response.put("messages", structuredResponse.getMessages());
            } else {
                response.put("success", false);
                response.put("message", "Structured Outputs 테스트 실패 - null 응답");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Structured Outputs 하루 마무리 메시지 테스트 중 오류 발생", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "테스트 중 오류가 발생했습니다.");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
    
    /**
     * Structured Outputs 전체 실행 테스트
     * 실제 알림 시스템에서 사용할 메시지들을 Structured Outputs로 생성 및 저장
     */
    @PostMapping("/test/structured-generate")
    public ResponseEntity<Map<String, Object>> generateStructuredMessages() {
        try {
            if (!openAIInternalService.isServiceEnabled()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "OpenAI 서비스가 비활성화되어 있습니다.");
                
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("Structured Outputs 전체 메시지 생성 테스트 시작");
            
            Map<String, Object> response = new HashMap<>();
            
            // 백그라운드에서 실행하여 응답 지연 방지
            new Thread(() -> {
                try {
                    log.info("잔소리 메시지 Structured Outputs 생성 시작");
                    openAIInternalService.generateAndStoreStructuredNaggingMessages();
                    
                    log.info("하루 마무리 메시지 Structured Outputs 생성 시작");
                    openAIInternalService.generateAndStoreStructuredWrapUpMessages();
                    
                    log.info("Structured Outputs 전체 메시지 생성 완료");
                    
                } catch (Exception e) {
                    log.error("백그라운드 Structured Outputs 메시지 생성 중 오류 발생", e);
                }
            }).start();
            
            response.put("success", true);
            response.put("message", "Structured Outputs 메시지 생성이 백그라운드에서 시작");
            response.put("info", "잔소리 메시지와 하루 마무리 메시지를 모두 생성");
            response.put("checkEndpoints", List.of("/api/openai/daily-answers", "/api/openai/daily-wrapup-answers"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Structured Outputs 전체 메시지 생성 중 오류 발생", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "메시지 생성 중 오류 발생");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
    
    /**
     * 단일 프롬프트 테스트 (기존 방식과 비교용)
     */
    @PostMapping("/test/single-prompt")
    public ResponseEntity<Map<String, Object>> testSinglePrompt(@RequestBody Map<String, String> request) {
        try {
            if (!openAIInternalService.isServiceEnabled()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "OpenAI 서비스가 비활성화되어 있습니다~");
                
                return ResponseEntity.badRequest().body(response);
            }
            
            String prompt = request.get("prompt");
            if (prompt == null || prompt.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "프롬프트가 필요합니다~");
                
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("단일 프롬프트 테스트: {}", prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt);
            
            String result = openAIInternalService.testSinglePrompt(prompt);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("prompt", prompt);
            response.put("result", result);
            response.put("type", "single_prompt");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("단일 프롬프트 테스트 중 오류 발생", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "테스트 중 오류가 발생했습니다~");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
    
    /**
     * 디버깅용 - 현재 저장된 메시지 상태 확인
     */
    @GetMapping("/debug/messages")
    public ResponseEntity<Map<String, Object>> debugMessages() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("serviceEnabled", openAIInternalService.isServiceEnabled());
            response.put("dailyAnswersCount", openAIInternalService.getDailyAnswers().size());
            response.put("wrapUpAnswersCount", openAIInternalService.getDailyWrapUpAnswers().size());
            
            // 원본 메시지들
            List<String> dailyAnswers = openAIInternalService.getDailyAnswers();
            List<String> wrapUpAnswers = openAIInternalService.getDailyWrapUpAnswers();
            
            response.put("dailyAnswersRaw", dailyAnswers);
            response.put("wrapUpAnswersRaw", wrapUpAnswers);
            
            // 첫 번째 메시지 분석
            if (!wrapUpAnswers.isEmpty()) {
                String firstAnswer = wrapUpAnswers.get(0);
                String[] splitMessages = firstAnswer.split("\\n");
                
                response.put("firstWrapUpAnswer", firstAnswer);
                response.put("splitMessagesCount", splitMessages.length);
                response.put("splitMessages", List.of(splitMessages));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("디버깅 정보 조회 중 오류 발생", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "디버깅 정보 조회 중 오류가 발생했습니다~");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
}