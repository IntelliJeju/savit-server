package com.savit.challenge.service;

import com.savit.budget.domain.CategoryVO;
import com.savit.budget.mapper.CategoryMapper;
import com.savit.card.mapper.CardApprovalMapper;
import com.savit.card.mapper.CardTransactionMapper;
import com.savit.challenge.domain.ChallengeVO;
import com.savit.challenge.dto.ChallengeDetailDTO;
import com.savit.challenge.dto.ChallengeListDTO;
import com.savit.challenge.dto.ChallengeSummaryDTO;
import com.savit.challenge.mapper.ChallengeMapper;
import com.savit.challenge.mapper.ChallengeParticipationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeMapper challengeMapper;
    private final CategoryMapper categoryMapper;
    private final CardApprovalMapper cardMapper;
    private final CardTransactionMapper cardTransactionMapper;
    private final ChallengeParticipationMapper challengeParticipationMapper;

    // 리스트 조회
    @Override
    public List<ChallengeListDTO> getChallengeList(Long userId) {
        List<Long> categoryids = challengeMapper.findSuccessfulWeeklyCategories(userId);

        List<ChallengeListDTO> weeklyList = challengeMapper.findWeeklyChallenges(userId);

        if (!categoryids.isEmpty()) {
            for (Long categoryid : categoryids) {
                List<ChallengeListDTO> monthlyList = challengeMapper.findMonthlyChallenges(userId, categoryid);
                weeklyList.addAll(monthlyList);
            }
        }

        return weeklyList;
    }

    // 상세조회
    @Override
    public ChallengeDetailDTO getChallengeDetail(Long challengeId, Long userId) {
        // 1. challenge_id로 챌린지 정보 가져오기
        ChallengeVO challengeVO = challengeMapper.findById(challengeId);
        if (challengeVO == null) {
            throw new RuntimeException("챌린지 없음");
        }

        // 2. challengeVO로 카테고리 정보 가져오기
        CategoryVO categoryVO = categoryMapper.findById(challengeVO.getCategoryId());
        if (categoryVO == null) {
            throw new RuntimeException("카테고리 없음");
        }

        // 3. entry_fee 계산
        BigDecimal totalEntryFee = challengeParticipationMapper.sumMyFeeByChallenge(challengeId);
        if (totalEntryFee == null) {
            totalEntryFee = BigDecimal.ZERO;
        }


        // 4. 조건검사: 1/0
        int eligibility = checkEligibility(challengeVO, userId);

        String startDateStr = challengeVO.getStartDate() != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(challengeVO.getStartDate())
                : null;
        String endDateStr = challengeVO.getEndDate() != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(challengeVO.getEndDate())
                : null;

        // 5. 이제 response 타입인 challengeDetailDTO로 변환해주기
        return ChallengeDetailDTO.builder()
                .title(challengeVO.getTitle())
                .description(challengeVO.getDescription())
                .startDate(startDateStr)
                .endDate(endDateStr)
                .entryFee(totalEntryFee)
                .categoryName(categoryVO.getName())
                .durationWeeks(challengeVO.getDurationWeeks())
                .totalParticipants(challengeVO.getTotalParticipants())
                .joinedParticipants(challengeVO.getJoinedParticipants())
                .eligibility(eligibility).build();
    }

    // 조건검사. 결과 참가 가능이면 1, 아니면 0 반환
    @Override
    public int checkEligibility(ChallengeVO vo, Long userId) {

        // 1. userId로 사용자 카드(들) 가져오기
        List<Long> cardIds = cardMapper.findCardIdsByUser(userId);
        if (cardIds.isEmpty()) {
            return 0;
        }

        // 2. 기간 계산( 1주 챌린지 -> 3주 / 4주 챌린지 -> 12주)
        int previousWeeks = vo.getDurationWeeks() == 1 ? 3 : 12;

        // 3. 파라미터 설정
        Map<String, Object> params = new HashMap<>();
        params.put("cardIds", cardIds);
        params.put("categoryId", vo.getCategoryId());
        params.put("previousWeeks", previousWeeks);

            // 금액 기준 - type: AMOUNT
        if("AMOUNT".equals(vo.getType())) {
            BigDecimal totalAmount = cardTransactionMapper.sumAmountByParams(params);
            if (totalAmount == null) totalAmount = BigDecimal.ZERO;

            BigDecimal baselineAmount = vo.getTargetAmount().multiply(new BigDecimal("3"));
            return totalAmount.compareTo(baselineAmount) > 0 ? 1 : 0;

            // 횟수 기준 - type: COUNT
        } else if ("COUNT".equals(vo.getType())) {
            Long totalCount = cardTransactionMapper.countByParams(params);
            if (totalCount == null ) totalCount = 0L;

            Long baselineCount = (long) (vo.getTargetCount() * 3);
            return totalCount > baselineCount ? 1: 0;

        }
        return 0;
    }
    @Override
    public List<ChallengeListDTO> getParticipatingChallenges(Long userId) {
        return challengeMapper.findParticipatingChallenges(userId);
    }

    @Override
    public List<ChallengeSummaryDTO> getChallengeSummary(Long userId) {
        return challengeParticipationMapper.selectChallengeSummary(userId);
    }

}
