package com.savit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 내부 메서드 호출 방식 - 스케줄러 및 비동기 처리 설정
 * 카드 승인내역 자동 동기화 및 FCM 알림을 위한 스케줄러 활성화
 */
@Configuration
@EnableScheduling    // 스케줄러 활성화
@EnableAsync        // 비동기 처리 활성화
public class SchedulerConfig {
    
    /**
     * 내부 메서드 호출 방식 - 비동기 작업용 스레드 풀 설정
     * 카드 승인내역 동기화 시 외부 API 호출을 병렬 처리하기 위해 사용
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 기본 스레드 수 (항상 유지되는 스레드)
        executor.setCorePoolSize(5);
        
        // 최대 스레드 수 (피크 시간에 생성 가능한 최대 스레드)
        executor.setMaxPoolSize(15);
        
        // 대기 큐 크기 (스레드가 모두 사용 중일 때 대기할 작업 수)
        executor.setQueueCapacity(100);
        
        // 스레드 이름 접두사 (로그에서 식별 용이)
        executor.setThreadNamePrefix("CardSync-");
        
        // 스레드 유지 시간 (idle 스레드가 종료되기까지의 시간, 초)
        executor.setKeepAliveSeconds(60);
        
        // 애플리케이션 종료 시 실행 중인 작업 완료까지 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 종료 대기 시간 (초)
        executor.setAwaitTerminationSeconds(30);
        
        // 스레드 풀 초기화
        executor.initialize();
        
        return executor;
    }

    @Bean
    public org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler taskScheduler() {
        var ts = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        ts.setPoolSize(4);
        ts.setThreadNamePrefix("sched-");
        ts.setWaitForTasksToCompleteOnShutdown(true);
        ts.setAwaitTerminationSeconds(30);
        return ts;
    }

}