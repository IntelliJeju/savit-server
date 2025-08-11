package com.savit.challenge.service;

import com.savit.card.domain.CardTransactionVO;
import com.savit.challenge.dto.ChallengeProgressDTO;
import com.savit.challenge.dto.ChallengeUpdateRequestDTO;
import com.savit.challenge.dto.ParticipationStatusDTO;
import com.savit.challenge.dto.ChallengeFailedParticipantDTO;

import java.util.List;

public interface ChallengeParticipationService {
    void updateChallengeProgressForNewTransactions();
    List<CardTransactionVO> findNewTransactionsToProcess();
    void processSingleTransaction(CardTransactionVO transaction);
    String getLastSchedulerTime();
    List<ParticipationStatusDTO> findActiveParticipantsByCategory(Long categoryId);
    void processParticipantProgress(ParticipationStatusDTO participant, CardTransactionVO transaction);
    ChallengeUpdateRequestDTO createUpdateRequest(ChallengeProgressDTO progress);
    void updateParticipationStatus(ChallengeUpdateRequestDTO updateRequest);
    
    /**
     * 새로 실패한 참여자 목록 조회 (알림 발송용)
     * 오늘 실패로 변경된 참여자들만 반환
     */
    List<ChallengeFailedParticipantDTO> findNewlyFailedParticipants();
}
