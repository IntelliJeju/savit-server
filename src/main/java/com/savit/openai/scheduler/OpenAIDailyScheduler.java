package com.savit.openai.scheduler;

import com.savit.openai.service.OpenAIInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 01:00에 OpenAI 답변을 갱신하는 스케줄러
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIDailyScheduler {
    
    private final OpenAIInternalService openAIInternalService;
    
    /**
     * 매일 오전 1시에 실행되는 스케줄러
     * 테스트 위해 주석처리
     */
//    @Scheduled(cron = "0 0 1 * * ?")
    public void refreshDailyAnswers() {
        log.info("=== OpenAI 일일 답변 갱신 스케줄러 시작 ===");
        
        try {
            if (!openAIInternalService.isServiceEnabled()) {
                log.warn("OpenAI 서비스가 비활성화되어 있어 스케줄러를 건너뜁니다.");
                return;
            }
            
            long startTime = System.currentTimeMillis();
            
            // OpenAI 랜덤 잔소리 답변 생성 및 저장
            openAIInternalService.generateAndStoreDailyAnswers();
            
            // OpenAI 하루 마무리 답변 생성 및 저장
            openAIInternalService.generateAndStoreDailyWrapUpAnswers();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            log.info("=== OpenAI 일일 답변 갱신 완료 ===");
            log.info("처리 시간: {}ms ({}초)", duration, duration / 1000.0);
            log.info("생성된 랜덤 잔소리 답변 수: {}", openAIInternalService.getAnswerCount());
            log.info("생성된 하루 마무리 답변 수: {}", openAIInternalService.getDailyWrapUpAnswers().size());
            
        } catch (Exception e) {
            log.error("OpenAI 일일 답변 갱신 중 오류 발생", e);
        }
    }
    
    /**
     * 테스트용 수동 실행 메서드 (개발 시 사용)
     * 운영시 주석 처리
     */
    public void manualRefresh() {
        log.info("=== 수동 OpenAI 답변 갱신 시작 ===");
        refreshDailyAnswers();
    }
}