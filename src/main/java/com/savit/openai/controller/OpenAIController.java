package com.savit.openai.controller;

import com.savit.openai.service.OpenAIInternalService;
import com.savit.openai.scheduler.OpenAIDailyScheduler;
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
}