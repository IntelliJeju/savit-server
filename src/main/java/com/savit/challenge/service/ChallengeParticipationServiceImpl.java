package com.savit.challenge.service;

import com.savit.card.domain.CardTransactionVO;
import com.savit.card.mapper.CardTransactionMapper;
import com.savit.challenge.dto.ChallengeProgressDTO;
import com.savit.challenge.dto.ChallengeUpdateRequestDTO;
import com.savit.challenge.dto.ParticipationStatusDTO;
import com.savit.challenge.dto.ChallengeFailedParticipantDTO;
import com.savit.challenge.mapper.ChallengeParticipationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ChallengeParticipationServiceImpl implements ChallengeParticipationService {

    private final CardTransactionMapper cardTransactionMapper;
    private final ChallengeParticipationMapper challengeParticipationMapper;
    private final ChallengeProgressService challengeProgressService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChallengeProgressForNewTransactions() {
        try {
            log.info("🎯 ====챌린지 업데이트 시작 (상세 디버깅)======");

            // 1. 거래 내역 조회
            List<CardTransactionVO> newTransactions = findNewTransactionsToProcess();

            if (newTransactions.isEmpty()) {
                log.warn("⚠️ 처리할 새로운 결제 내역이 없습니다.");
                return;
            }
            log.info("💳 처리 대상 새로운 결제 내역: {} 건", newTransactions.size());

            // 거래내역 상세 출력
            for (int i = 0; i < newTransactions.size(); i++) {
                CardTransactionVO tx = newTransactions.get(i);
                log.info("📋 거래[{}] - ID: {}, 카드: {}, 카테고리: {}, 금액: {}, 매장: {}, 취소: {}, 생성: {}",
                        i+1, tx.getId(), tx.getCardId(), tx.getCategoryId(),
                        tx.getResUsedAmount(), tx.getResMemberStoreName(),
                        tx.getResCancelYn(), tx.getCreatedAt());
            }

            // 2. 각 거래별로 처리
            int processedCount = 0;
            for (CardTransactionVO transaction : newTransactions) {
                try {
                    log.info("🔄 === 거래 {} 처리 시작 ===", transaction.getId());
                    processSingleTransaction(transaction);
                    processedCount++;
                    log.info("✅ === 거래 {} 처리 완료 ===", transaction.getId());
                } catch (Exception e) {
                    log.error("❌ 거래 처리 실패 - 거래ID: {}", transaction.getId(), e);
                    // 예외 재발생으로 트랜잭션 롤백 유도
                    throw new RuntimeException("거래 처리 중 오류 발생", e);
                }
            }

            log.info("✅ ====챌린지 상태 업데이트 완료 - 처리: {} 건 / 전체: {} 건 =====", processedCount, newTransactions.size());

        } catch (Exception e) {
            log.error("🚨 챌린지 상태 업데이트 중 전체 오류 발생 - 트랜잭션 롤백!", e);
            throw new RuntimeException("챌린지 상태 업데이트 실패", e);
        }
    }

    @Override
    public List<CardTransactionVO> findNewTransactionsToProcess() {
        String lastSchedulerTime = getLastSchedulerTime();
        String currentTime = LocalDateTime.now().toString();

        log.info("🔍 거래조회범위 - 시작: {}, 종료: {}", lastSchedulerTime, currentTime);

        List<CardTransactionVO> transactions = cardTransactionMapper.findTransactionBetweenTime(lastSchedulerTime, currentTime);

        log.info("📊 조회된 거래 수: {} 건", transactions.size());

        return transactions;
    }

    @Override
    public String getLastSchedulerTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        LocalDate currentDate = now.toLocalDate();

        LocalDateTime lastSchedulerTime;

        if (currentTime.isBefore(LocalTime.of(6, 0))) {
            lastSchedulerTime = currentDate.minusDays(1).atTime(0, 0);
        } else if (currentTime.isBefore(LocalTime.of(12, 0))) {
            lastSchedulerTime = currentDate.atTime(6, 0);
        } else if (currentTime.isBefore(LocalTime.of(18, 0))) {
            lastSchedulerTime = currentDate.atTime(12, 0);
        } else {
            lastSchedulerTime = currentDate.atTime(18, 0);
        }

        log.debug("⏰ 스케줄러 시간 계산 - 현재: {}, 마지막: {}", now, lastSchedulerTime);

        return lastSchedulerTime.toString();
    }

    @Override
    @Transactional
    public void processSingleTransaction(CardTransactionVO transaction) {
        try {
            log.info("💰 단일 거래 처리 시작 - ID: {}, 카테고리: {}, 금액: {}",
                    transaction.getId(), transaction.getCategoryId(), transaction.getResUsedAmount());

            // 취소된 거래 체크
            if ("Y".equals(transaction.getResCancelYn())) {
                log.info("🚫 취소된 거래 스킵 - 거래ID: {}", transaction.getId());
                return;
            }

            // 카테고리 체크
            if (transaction.getCategoryId() == null) {
                log.info("❓ 카테고리 미분류 거래 스킵 - 거래ID: {}", transaction.getId());
                return;
            }

            log.info("🔍 카테고리 {} 진행중인 참여자 조회 중...", transaction.getCategoryId());

            // 1. 해당 카테고리의 진행중인 챌린지 참여자들 조회
            List<ParticipationStatusDTO> activeParticipants = findActiveParticipantsByCategory(transaction.getCategoryId());

            log.info("👥 카테고리 {} 진행중인 참여자: {} 명", transaction.getCategoryId(), activeParticipants.size());

            if (activeParticipants.isEmpty()) {
                log.info("⚠️ 해당 카테고리의 진행중인 참여자 없음 - 카테고리: {}", transaction.getCategoryId());
                return;
            }

            // 참여자 정보 상세 출력
            for (int i = 0; i < activeParticipants.size(); i++) {
                ParticipationStatusDTO p = activeParticipants.get(i);
                log.info("👤 참여자[{}] - ID: {}, 사용자: {}, 챌린지: {}, 타입: {}, 현재: {} {}, 목표: {} {}",
                        i+1, p.getParticipationId(), p.getUserId(), p.getChallengeId(), p.getType(),
                        p.getType().equals("COUNT") ? p.getCurrentCount() : p.getCurrentAmount(),
                        p.getType().equals("COUNT") ? "회" : "원",
                        p.getType().equals("COUNT") ? p.getTargetCount() : p.getTargetAmount(),
                        p.getType().equals("COUNT") ? "회" : "원");
            }

            // 2. 각 참여자별로 진행상황 업데이트
            for (ParticipationStatusDTO participant : activeParticipants) {
                try {
                    log.info("🎯 참여자 {} 진행상황 처리 시작", participant.getParticipationId());
                    processParticipantProgress(participant, transaction);
                    log.info("✅ 참여자 {} 진행상황 처리 완료", participant.getParticipationId());
                } catch (Exception e) {
                    log.error("❌ 참여자 처리 실패 - 참여ID: {}", participant.getParticipationId(), e);
                    // 참여자 처리 실패 시에도 전체 트랜잭션 롤백
                    throw new RuntimeException("참여자 처리 실패", e);
                }
            }
        } catch (Exception e) {
            log.error("❌ 단일 거래 처리 실패 - 거래ID: {}", transaction.getId(), e);
            throw e;
        }
    }

    @Override
    public List<ParticipationStatusDTO> findActiveParticipantsByCategory(Long categoryId) {
        try {
            log.info("🔍 카테고리 {} 활성 참여자 조회 시작", categoryId);

            List<ParticipationStatusDTO> participants =
                    challengeParticipationMapper.findActiveParticipantsByCategory(categoryId);

            log.info("📊 카테고리 {} 활성 참여자 조회 결과: {} 명", categoryId, participants.size());

            return participants;
        } catch (Exception e) {
            log.error("❌ 활성 참여자 조회 실패 - 카테고리: {}", categoryId, e);
            throw new RuntimeException("활성 참여자 조회 실패", e);
        }
    }

    @Override
    public void processParticipantProgress(ParticipationStatusDTO participant, CardTransactionVO transaction) {
        try {
            log.info("🧮 참여자 {} 진행상황 처리 - 거래 {}", participant.getParticipationId(), transaction.getId());

            // 1. 업데이트된 진행상황 계산
            log.info("📊 진행상황 계산 시작...");
            ChallengeProgressDTO progress = challengeProgressService.calculateUpdatedProgress(participant, transaction);

            log.info("📈 계산 완료 - 새상태: {}, 타입: {}, 업데이트값: {} {}, 목표초과: {}",
                    progress.getNewStatus(),
                    progress.getType(),
                    progress.getType().equals("COUNT") ? progress.getUpdatedCount() : progress.getUpdatedAmount(),
                    progress.getType().equals("COUNT") ? "회" : "원",
                    progress.isExceeded());

            // 2. 상태 업데이트 요청 생성
            log.info("📝 업데이트 요청 생성 중...");
            ChallengeUpdateRequestDTO updateRequest = createUpdateRequest(progress);

            log.info("📋 업데이트 요청 - 참여ID: {}, 상태: {}, 카운트: {}, 금액: {}, 완료시간: {}",
                    updateRequest.getParticipationId(),
                    updateRequest.getNewStatus(),
                    updateRequest.getUpdatedCount(),
                    updateRequest.getUpdatedAmount(),
                    updateRequest.getCompletedAt());

            // 3. DB 업데이트 실행
            log.info("💾 DB 업데이트 실행 중...");
            updateParticipationStatus(updateRequest);

            log.info("✅ 참여자 진행상황 업데이트 완료 - 참여ID: {}, 상태: {}, 누적: {} {}",
                    participant.getParticipationId(),
                    progress.getNewStatus(),
                    progress.getType().equals("COUNT") ? progress.getUpdatedCount() : progress.getUpdatedAmount(),
                    progress.getType().equals("COUNT") ? "회" : "원");

        } catch (Exception e) {
            log.error("❌ 참여자 진행상황 처리 실패 - 참여ID: {}", participant.getParticipationId(), e);
            throw new RuntimeException("참여자 진행상황 처리 실패", e);
        }
    }

    @Override
    public ChallengeUpdateRequestDTO createUpdateRequest(ChallengeProgressDTO progress) {
        ChallengeUpdateRequestDTO.ChallengeUpdateRequestDTOBuilder builder =
                ChallengeUpdateRequestDTO.builder()
                        .participationId(progress.getParticipationId())
                        .newStatus(progress.getNewStatus())
                        .updatedCount(progress.getUpdatedCount())
                        .updatedAmount(progress.getUpdatedAmount());

        if ("FAIL".equals(progress.getNewStatus())) {
            builder.completedAt(LocalDateTime.now());
        }
        return builder.build();
    }

    @Override
    public void updateParticipationStatus(ChallengeUpdateRequestDTO updateRequest) {
        try {
            log.info("💾 DB 업데이트 시작 - 참여ID: {}, 상태: {}",
                    updateRequest.getParticipationId(), updateRequest.getNewStatus());

            if ("FAIL".equals(updateRequest.getNewStatus())) {
                log.info("❌ FAIL 상태 업데이트 실행 중...");
                challengeParticipationMapper.updateStatusToFail(updateRequest);
                log.info("✅ FAIL 상태 업데이트 완료");
            } else {
                log.info("📈 진행상황 업데이트 실행 중 - 카운트: {}, 금액: {}",
                        updateRequest.getUpdatedCount(), updateRequest.getUpdatedAmount());
                challengeParticipationMapper.updateChallengeProgress(
                        updateRequest.getParticipationId(),
                        updateRequest.getUpdatedCount(),
                        updateRequest.getUpdatedAmount());
                log.info("✅ 진행상황 업데이트 완료");
            }

        } catch (Exception e) {
            log.error("❌ DB 업데이트 실패 - 참여ID: {}", updateRequest.getParticipationId(), e);
            throw new RuntimeException("DB 업데이트 실패", e);
        }
    }

    @Override
    public List<ChallengeFailedParticipantDTO> findNewlyFailedParticipants() {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            log.info("🔍 새로 실패한 챌린지 참여자 조회 시작 - 대상날짜: {}", today);
            
            // 오늘 FAIL로 변경된 참여자들 조회 (완료시간이 오늘인 FAIL 상태)
            List<ChallengeFailedParticipantDTO> failedParticipants = challengeParticipationMapper.findNewlyFailedParticipants(today);
            
            log.info("📊 새로 실패한 챌린지 참여자 조회 완료 - {}명", failedParticipants.size());
            
            if (!failedParticipants.isEmpty()) {
                for (int i = 0; i < failedParticipants.size(); i++) {
                    ChallengeFailedParticipantDTO participant = failedParticipants.get(i);
                    log.info("❌ 실패참여자[{}] - 사용자: {}, 챌린지: '{}', 상태: {}", 
                            i+1, participant.getUserId(), participant.getChallengeTitle(), participant.getStatus());
                }
            }
            
            return failedParticipants;
            
        } catch (Exception e) {
            log.error("❌ 새로 실패한 참여자 조회 실패", e);
            // 예외 발생 시 빈 리스트 반환하여 알림 프로세스가 중단되지 않도록 함
            return new ArrayList<>();
        }
    }
}