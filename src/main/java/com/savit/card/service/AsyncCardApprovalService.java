package com.savit.card.service;

import com.savit.budget.service.BudgetMonitoringService;
import com.savit.challenge.mapper.ChallengeParticipationMapper;
import com.savit.challenge.service.ChallengeParticipationService;
import com.savit.challenge.dto.ChallengeFailedParticipantDTO;
import com.savit.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 내부 메서드 호출 방식 - 비동기 카드 승인내역 처리 서비스
 * codef 외부 api 호출로 인한 블로킹을 방지하기 위해 비동기로 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncCardApprovalService {
    
    private final CardApprovalService cardApprovalService;
    private final BudgetMonitoringService budgetMonitoringService;
    private final ChallengeParticipationService challengeParticipationService;
    private final NotificationService notificationService;
    
    /**
     * 내부 메서드 호출 방식 - 단일 사용자 카드 승인내역 비동기 처리
     * 스케줄러에서 각 사용자별로 병렬 처리하기 위해 사용
     * 각 사용자 카드 돌면서 승인내역 업데이트
     * 업데이트 사항 존재시 예산 체크
     */
    @Async
    public CompletableFuture<Void> processUserCardApprovalsAsync(Long userId) {
        try {
            log.info("사용자 {} 카드 승인내역 비동기 처리 시작", userId);
            
            // 내부 메서드 호출 방식 - 사용자의 모든 카드 처리
            boolean hasNewTransactions = cardApprovalService.fetchAndSaveAllCardsWithBudgetCheck(userId);

            // 새로운 거래내역이 있으면 예산 체크 및 알림 발송
            if (hasNewTransactions) {
                log.info("사용자 {} 새 거래내역 발견 - 예산 모니터링 시작", userId);
                budgetMonitoringService.checkBudgetAndSendNotifications(userId);

                // 챌린지 참여자 상태 업데이트
                try {
                    challengeParticipationService.updateChallengeProgressForNewTransactions();
                    log.debug("사용자 {} 챌린지 상태 업데이트 완료", userId);
                    
                    // 새로 실패한 참여자들에게 실패 알림 발송
                    sendChallengeFailNotifications();
                    
                } catch (Exception e) {
                    log.error("사용자 {} 챌린지 상태 업데이트 실패 - 예산 모니터링은 정상 처리됨", userId,e);
                    // 챌린지 처리 실패가 전체 프로세스 중단시키지 않도록 continue~
                }
            } else {
                log.debug("사용자 {} 새 거래내역 없음", userId);
            }
            
            log.info("사용자 {} 카드 승인내역 비동기 처리 완료 - 새 거래내역: {}", userId, hasNewTransactions);
            
        } catch (Exception e) {
            log.error("사용자 {} 카드 승인내역 비동기 처리 실패: {}", userId, e.getMessage(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 내부 메서드 호출 방식 - 단일 카드 승인내역 비동기 처리 (단일 사용자의 개별 카드용)
     * 단일 사용자 특정 카드만 처리해야 하는 경우 사용
     */
    @Async
    public CompletableFuture<Void> processSingleCardApprovalsAsync(Long userId, Long cardId) {
        try {
            log.info("사용자 {} 카드 {} 승인내역 비동기 처리 시작", userId, cardId);
            
            // 내부 메서드 호출 방식 - 특정 카드 처리
            boolean hasNewTransactions = cardApprovalService.fetchAndSaveApprovalsWithBudgetCheck(userId, cardId);
            
            // 새로운 거래내역이 있으면 예산 체크 및 알림 발송
            if (hasNewTransactions) {
                log.info("사용자 {} 카드 {} 새 거래내역 발견 - 예산 모니터링 시작", userId, cardId);
                budgetMonitoringService.checkBudgetAndSendNotifications(userId);
                try {
                    challengeParticipationService.updateChallengeProgressForNewTransactions();
                    log.debug("사용자 {} 카드 {} 챌린지 상태 업데이트 완료", userId, cardId);
                    
                    // 새로 실패한 참여자들에게 실패 알림 발송
                    sendChallengeFailNotifications();
                    
                } catch (Exception e) {
                    log.error("사용자 {} 카드 {} 챌린지 상태 업데이트 실패", userId, cardId, e);
                }
            } else {
                log.debug("사용자 {} 카드 {} 새 거래내역 없음", userId, cardId);
            }
            
            log.info("사용자 {} 카드 {} 승인내역 비동기 처리 완료 - 새 거래내역: {}", userId, cardId, hasNewTransactions);
            
        } catch (Exception e) {
            log.error("사용자 {} 카드 {} 승인내역 비동기 처리 실패: {}", userId, cardId, e.getMessage(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 내부 메서드 호출 방식 - 비동기 처리 상태 로깅
     * 스케줄러에서 전체 처리 현황을 파악하기 위해 사용
     */
    public void logAsyncProcessingStatus(int totalUsers, int processedUsers) {
        log.info("비동기 카드 승인내역 처리 현황 - 전체: {}명, 처리 시작: {}명", totalUsers, processedUsers);
    }
    
    /**
     * 새로 실패한 챌린지 참여자들에게 실패 알림 발송
     * 중복 알림 방지 로직 포함
     */
    private void sendChallengeFailNotifications() {
        try {
            List<ChallengeFailedParticipantDTO> failedParticipants = challengeParticipationService.findNewlyFailedParticipants();
            
            if (failedParticipants.isEmpty()) {
                log.debug("새로 실패한 챌린지 참여자가 없습니다.");
                return;
            }
            
            log.info("새로 실패한 챌린지 참여자 {}명에게 알림 발송 시작", failedParticipants.size());
            
            int sentCount = 0;
            for (ChallengeFailedParticipantDTO participant : failedParticipants) {
                try {
                    // 실제 운영용 메서드 호출 (중복 방지 로직 포함)
                    notificationService.sendChallengeFailNotification(
                        participant.getUserId(),
                        participant.getChallengeId(),
                        participant.getChallengeTitle()
                    );
                    sentCount++;
                    
                    // 알림 발송 간격 조절
                    Thread.sleep(300);
                    
                } catch (Exception e) {
                    log.error("사용자 {} 챌린지 '{}' 실패 알림 발송 실패", 
                            participant.getUserId(), participant.getChallengeTitle(), e);
                }
            }
            
            log.info("챌린지 실패 알림 발송 완료 - 대상: {}명, 발송: {}명", failedParticipants.size(), sentCount);
            
        } catch (Exception e) {
            log.error("챌린지 실패 알림 발송 처리 중 오류 발생", e);
        }
    }
}