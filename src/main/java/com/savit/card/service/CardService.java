package com.savit.card.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.savit.card.domain.Card;
import com.savit.card.dto.CardDetailResponseDTO;
import com.savit.card.dto.CardRegisterRequestDTO;
import com.savit.card.mapper.CardMapper;
import com.savit.card.util.CodefUtil;
import io.codef.api.EasyCodef;
import io.codef.api.EasyCodefServiceType;
import io.codef.api.EasyCodefTokenMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional; // ✅ 추가

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CodefTokenService codefTokenService;
    private final CodefUtil codefUtil;
    private final CardMapper cardMapper;

    public String registerAccount(CardRegisterRequestDTO req) throws Exception {

        String accessToken = codefTokenService.getAccessToken();

        EasyCodefTokenMap.setToken(codefUtil.getClientId(), accessToken);

        EasyCodef client = codefUtil.newClient();

        HashMap<String, Object> account = new HashMap<>();
        account.put("countryCode", "KR");
        account.put("businessType", "CD");
        account.put("clientType", "P");
        account.put("organization", req.getOrganization());
        account.put("loginType", "1");
        account.put("loginTypeLevel", "2");
        account.put("id", req.getLoginId());
        // 카드사 비밀번호 테스트용으로 여기서 암호화 진행
        // 추후 req.fetLoginPw() 만 사용
        if (req.getLoginPw() == null) {
            throw new IllegalArgumentException("로그인 비밀번호가 필요합니다.");
        }

        account.put("password", codefUtil.encryptRSA(req.getLoginPw()));
        account.put("birthDate", req.getBirthDate());

        List<HashMap<String, Object>> list = List.of(account);
        HashMap<String, Object> params = new HashMap<>();
        params.put("accountList", list);

        String resp = client.createAccount(EasyCodefServiceType.DEMO, params);

        Map<?, ?> map = new ObjectMapper().readValue(resp, Map.class);
        Map<?, ?> data = (Map<?, ?>) map.get("data");
        String connectedId = (String) data.get("connectedId");

        if (connectedId == null) {
            throw new IllegalStateException("ConnectedId 발급 실패\n" + resp);
        }
        return connectedId;
    }

    public List<Map<String, Object>> fetchCardList(String connectedId,
                                                   String organization,
                                                   String birthDate) throws Exception {

        String accessToken = codefTokenService.getAccessToken();

        EasyCodefTokenMap.setToken(codefUtil.getClientId(), accessToken);

        EasyCodef client = codefUtil.newClient();

        HashMap<String, Object> params = new HashMap<>();
        params.put("connectedId", connectedId);
        params.put("organization", organization);
        params.put("birthDate", birthDate);
        params.put("inquiryType", "1");

        String resp = client.requestProduct(
                "/v1/kr/card/p/account/card-list",
                EasyCodefServiceType.DEMO,
                params
        );
        log.info("[CODEF] createAccount 응답 = {}", resp);

        Map<String, Object> map = new ObjectMapper().readValue(resp, Map.class);
        Object dataObj = map.get("data");

        List<Map<String, Object>> cardList;

        if (dataObj instanceof List) {
            cardList = (List<Map<String, Object>>) dataObj;
        } else if (dataObj instanceof Map) {
            cardList = List.of((Map<String, Object>) dataObj);
        } else {
            throw new IllegalStateException("예상치 못한 카드 응답 형식: " + dataObj);
        }

        return cardList;
    }

    private String digitsOnly(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private String userLast4(String userInput) {
        String d = digitsOnly(userInput);
        return d.length() >= 4 ? d.substring(d.length() - 4) : d;
    }

    private String visiblePrefix(String masked, int n) {
        if (masked == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : masked.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
            else if (c == '*') break; // 마스킹 시작되면 중단
        }
        String all = sb.toString();
        return all.length() > n ? all.substring(0, n) : all;
    }

    private String visibleSuffix(String masked, int n) {
        if (masked == null) return "";
        StringBuilder revDigits = new StringBuilder();
        for (int i = masked.length() - 1; i >= 0; i--) {
            char c = masked.charAt(i);
            if (Character.isDigit(c)) revDigits.append(c);
            else if (c == '*') break;
        }
        String suf = revDigits.reverse().toString();
        return suf.length() > n ? suf.substring(suf.length() - n) : suf;
    }

    public boolean matchesUserCardMasked(Map<String, Object> codefCard,
                                         String requestedOrganization,
                                         String userInputCardNo) {
        String orgInResp = (String) (codefCard.getOrDefault("organization", codefCard.get("resIssuerCode")));
        if (orgInResp != null && requestedOrganization != null && !requestedOrganization.equals(orgInResp)) {
            return false;
        }

        String masked = (String) codefCard.get("resCardNo");
        String visPrefix = visiblePrefix(masked, 8);
        String visSuffix = visibleSuffix(masked, 4);

        String userDigits = digitsOnly(userInputCardNo);
        if (userDigits.length() < 10) return false;

        String userPrefix8 = userDigits.substring(0, Math.min(8, userDigits.length()));
        String userSuf4    = userLast4(userDigits);

        boolean prefixOk = visPrefix.isEmpty() || userPrefix8.startsWith(visPrefix);
        boolean suffixOk = visSuffix.isEmpty() || userSuf4.endsWith(visSuffix);

        return prefixOk && suffixOk;
    }

    public Optional<Map<String, Object>> pickOneMasked(List<Map<String, Object>> cardList,
                                                       String organization,
                                                       String userInputCardNo) {
        return cardList.stream()
                .filter(c -> matchesUserCardMasked(c, organization, userInputCardNo))
                .findFirst();
    }

    public void saveCards(List<Map<String, Object>> cardDataList,
                          String connectedId,
                          String organization,
                          Long userId,
                          String encryptedCardNo,
                          String cardPassword) {

        List<Card> cards = cardDataList.stream()
                .filter(data -> {
                    String resCardNo = (String) data.get("resCardNo");
                    return !cardMapper.existsCardByResCardNoAndUserId(resCardNo, userId);
                })
                .map(data -> Card.builder()
                        .connectedId(connectedId)
                        .organization(organization)
                        .cardName((String) data.get("resCardName"))
                        .issuer((String) data.get("issuer"))
                        // 사용자입력부 평문 입력 -> 프론트에서 encryptRSA 한 값 받아옴
                        // 테스트 위해 임시로 codefUtil.encryptRSA(encryptedCardNo) 사용
                        // 추후 .encryptedCardNo(encryptedCardNo) 로 변경해야 함
                        .encryptedCardNo(codefUtil.encryptRSA(encryptedCardNo))
                        .resCardNo((String) data.get("resCardNo"))
                        .resCardType((String) data.get("resCardType"))
                        .resSleepYn((String) data.get("resSleepYn"))
                        // 비번도 마찬가지로 추후 변경
                        .cardPassword(codefUtil.encryptRSA(cardPassword))
                        .registeredAt(LocalDateTime.now())
                        .userId(userId)
                        .resImageLink((String) data.get("resImageLink"))
                        .build())
                .toList();

        if (!cards.isEmpty()) {
            cardMapper.insertCards(cards);
        }
    }

    public CardDetailResponseDTO getCardDetailWithUsage(Long cardId, Long userId) {
        Card card = cardMapper.selectCardByIdAndUserId(cardId, userId);
        if (card == null) throw new IllegalArgumentException("카드를 찾을 수 없습니다.");

        int usageAmount = cardMapper.selectMonthlyUsageAmount(cardId);

        return new CardDetailResponseDTO(card, usageAmount);
    }

    public List<CardDetailResponseDTO> getCardListByUser(Long userId) {
        List<Card> cards = cardMapper.selectCardsByUserId(userId);

        return cards.stream().map(card -> {
            int usageAmount = cardMapper.selectMonthlyUsageAmount(card.getId());
            return new CardDetailResponseDTO(card, usageAmount);
        }).toList();
    }

    @Transactional
    public Card renameCardAndFetch(Long cardId, Long userId, String newName) {
        if (newName == null || newName.isBlank()) return null;

        int updated = cardMapper.updateCardName(cardId, userId, newName);
        if (updated != 1) return null;

        return cardMapper.selectCardBasicByIdAndUserId(cardId, userId);
    }
}
