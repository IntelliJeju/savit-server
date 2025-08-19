package com.savit.card.service;

import com.savit.budget.domain.CategoryVO;
import com.savit.budget.mapper.CategoryMapper;
import com.savit.card.domain.CardTransactionVO;
import com.savit.card.dto.CardTransactionDto;
import com.savit.card.dto.ManualCategoryRequest;
import com.savit.card.mapper.CardTransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardTransactionService {

    private final CardTransactionMapper cardTransactionMapper;
    private final CategoryMapper categoryMapper;

    private static final Map<String, List<String>> NAME_KEYWORDS = Map.ofEntries(
            Map.entry("식당", List.of("일반음식점", "패스트푸드", "맥도날드", "일반", "김밥", "한솥", "맘스터치", "본죽", "삼겹살", "국밥", "쌀국수", "고기", "마라탕", "우동", "돈까스", "비빔밥", "분식", "한식", "일식", "중식", "양식", "일반대중음식", "버거킹")),
            Map.entry("카페", List.of("스타벅스", "이디야", "컴포즈", "투썸", "커피", "베스킨", "메가", "폴바셋", "할리스", "블루보틀", "음료", "커피/음료전문점", "커피전문점", "공차")),
            Map.entry("배달", List.of("배달의민족", "요기요", "쿠팡이츠", "땡겨요", "배달앱", "배달전문", "배민", "우아한형제")),
            Map.entry("대중교통", List.of("버스", "지하철", "티머니", "교통카드", "환승", "대중교통", "버스/지하철")),
            Map.entry("철도", List.of("철도")),
            Map.entry("택시", List.of("카카오택시", "타다", "마카롱택시", "온다", "우버", "택시")),
            Map.entry("통신비", List.of("SKT", "KT", "LGU+", "알뜰폰", "요금", "통신사")),
            Map.entry("공과금", List.of("전기", "수도", "도시가스", "지역난방", "공공요금", "한전", "유플러스")),
            Map.entry("편의점/마트", List.of("GS25", "CU", "세븐일레븐", "이마트24", "다이소", "홈플러스", "롯데마트", "이마트", "마트", "할인점/슈퍼마켓", "할인점", "슈퍼마켓", "지에스")),
            Map.entry("공연", List.of("예술의전당", "뮤지컬", "콘서트", "연극", "공연", "티켓링크", "인터파크티켓", "공연장/극장", "극장", "놀", "놀유니버스")),
            Map.entry("쇼핑", List.of("쿠팡", "11번가", "지마켓", "G마켓", "SSG", "롯데ON", "네이버쇼핑", "마켓컬리", "백화점", "올리브영")),
            Map.entry("유흥", List.of("술집", "호프", "포차", "맥주", "노래방", "주점")),
            Map.entry("의료비", List.of("병원", "약국", "치과", "한의원", "병원비", "이비인후과", "내과", "안과", "정형외과", "피부과", "정신과", "의원", "개인병원", "외과")),
            Map.entry("교육", List.of("학원", "온라인강의", "인강", "과외", "교육비", "수강료", "토익", "자격증", "공부")),
            Map.entry("정기구독", List.of("넷플릭스", "netflix", "디즈니플러스", "disney", "왓챠", "유튜브프리미엄", "yt", "멜론", "벅스", "지니뮤직", "정기결제", "구독", "openai", "gpt", "youtube")),
            Map.entry("영화", List.of("CGV", "메가박스", "롯데시네마", "영화관", "영화티켓", "무비", "시네마", "씨지브이")),
            Map.entry("생활",List.of("미용","미용실","이발","이발소","블루클럽","헤어"))
    );

    private static final Map<String, String> SAFE_STORE_TYPE_MAPPING = Map.ofEntries(
            Map.entry("한식", "식당"),
            Map.entry("일식", "식당"),
            Map.entry("중식", "식당"),
            Map.entry("양식", "식당"),
            Map.entry("일반대중음식", "식당"),
            Map.entry("패스트푸드", "식당"),
            Map.entry("음료", "카페"),
            Map.entry("커피/음료전문점", "카페"),
            Map.entry("커피전문점", "카페"),
            Map.entry("버스/지하철", "대중교통"),
            Map.entry("철도", "철도"),
            Map.entry("공연장/극장", "공연"),
            Map.entry("극장", "공연"),
            Map.entry("공연", "공연"),
            Map.entry("노래방", "유흥"),
            Map.entry("백화점", "쇼핑"),
            Map.entry("개인병원", "의료비"),
            Map.entry("이비인후과", "의료비"),
            Map.entry("내과", "의료비"),
            Map.entry("외과", "의료비"),
            Map.entry("정형외과", "의료비"),
            Map.entry("할인점/슈퍼마켓", "편의점/마트"),
            Map.entry("할인점", "편의점/마트"),
            Map.entry("슈퍼마켓", "편의점/마트"),
            Map.entry("유플러스", "공과금"),
            Map.entry("놀", "공연"),
            Map.entry("놀유니버스", "공연"),
            Map.entry("씨지브이", "영화"),
            Map.entry("올리브영", "쇼핑"),
            Map.entry("버거킹", "식당"),
            Map.entry("공차", "카페"),
            Map.entry("지에스", "편의점/마트"),
            Map.entry("택시", "택시"),
            Map.entry("버스", "대중교통"),
            Map.entry("지하철", "대중교통"),
            Map.entry("편의점", "편의점/마트"),
            Map.entry("병원", "의료비"),
            Map.entry("학원", "교육"),
            Map.entry("넷플릭스", "정기구독"),
            Map.entry("CGV", "영화")
    );

    /**
     * 카드 승인 내역에 대해 카테고리를 자동 분류하여 업데이트
     * Codef API로 이미 저장된 승인 내역에 대해 실행되며,
     * cardId + 사용일자 + 사용시간 기준으로 해당 거래를 찾아 category/budget_category를 update 함
     */
    public void autoClassifyTransaction(CardTransactionDto dto) {
        Long userId = dto.getUserId();
        CategoryVO category = classifyCategory(dto.getResMemberStoreName(), dto.getResMemberStoreType());

        Long transactionId = cardTransactionMapper.findTransactionIdByCardIdAndDateTime(
                userId, dto.getCardId(), dto.getResUsedDate(), dto.getResUsedTime()
        );

        if (transactionId == null) {
            log.warn("해당 승인내역을 찾을 수 없습니다. userId={}, cardId={}, date={}, time={}",
                    userId, dto.getCardId(), dto.getResUsedDate(), dto.getResUsedTime());
            return;
        }

        cardTransactionMapper.updateCategory(transactionId, category.getId());
    }

    // 수동 카테고리 지정
    public void updateCategory(ManualCategoryRequest req) {
        Long userId = req.getUserId();
        Long transactionId = req.getTransactionId();
        Long categoryId = req.getCategoryId();

        if (!cardTransactionMapper.isOwnedByUser(transactionId, userId)) {
            throw new SecurityException("해당 거래는 사용자 소유가 아닙니다.");
        }

        CategoryVO category = categoryMapper.findById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("해당 카테고리 ID를 찾을 수 없습니다: " + categoryId);
        }

        cardTransactionMapper.updateCategory(transactionId, categoryId);
    }

    // 자동 재분류
    public int reclassifyUncategorizedTransactions(Long userId) {
        log.info("사용자 {}의 미분류 거래 조회 시작", userId);
        List<CardTransactionVO> transactions = cardTransactionMapper.findUnclassifiedTransactionsByUser(userId);
        log.info("미분류 거래 조회 결과: {}건", transactions.size());
        int updatedCount = 0;

        for (CardTransactionVO tx : transactions) {
            try {
                CategoryVO category = classifyCategory(tx.getResMemberStoreName(), tx.getResMemberStoreType());
                cardTransactionMapper.updateCategory(tx.getId(), category.getId());
                updatedCount++;
            } catch (Exception e) {
                log.warn("자동 분류 실패 - txId: {}, error: {}", tx.getId(), e.getMessage());
            }
        }

        return updatedCount;
    }


    // 분류 로직
    private CategoryVO classifyCategory(String storeName, String storeType) {
        if (storeName != null && !storeName.isBlank()) {
            String normalized = normalize(storeName);
            for (Map.Entry<String, List<String>> entry : NAME_KEYWORDS.entrySet()) {
                for (String keyword : entry.getValue()) {
                    if (normalized.contains(keyword.toLowerCase())) {
                        return getCategoryOrThrow(entry.getKey());
                    }
                }
            }
        }

        if (storeType != null && !storeType.isBlank()) {
            for (Map.Entry<String, String> entry : SAFE_STORE_TYPE_MAPPING.entrySet()) {
                if (storeType.contains(entry.getKey())) {
                    return getCategoryOrThrow(entry.getValue());
                }
            }
        }

        return getCategoryOrThrow("기타");
    }

    // 카테고리명으로 조회 실패 시 예외
    private CategoryVO getCategoryOrThrow(String name) {
        CategoryVO category = categoryMapper.findByName(name);
        if (category == null) {
            throw new IllegalStateException("'" + name + "' 카테고리가 DB에 존재하지 않습니다.");
        }
        return category;
    }

    // 상호명 전처리
    private String normalize(String text) {
        return text.replaceAll("[^가-힣a-zA-Z0-9]", "").toLowerCase();
    }
}
