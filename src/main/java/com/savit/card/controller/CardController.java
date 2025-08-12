package com.savit.card.controller;

import com.savit.card.dto.CardDetailResponseDTO;
import com.savit.card.dto.CardRegisterRequestDTO;
import com.savit.card.service.CardService;
import com.savit.card.service.ClovaOCRService;
import com.savit.security.JwtUtil;
import com.savit.user.domain.User;
import com.savit.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final ClovaOCRService clovaOCRService;
    @PostMapping(value="/register", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerCardAndFetch(
            @RequestParam("organization") String organization,
            @RequestParam("loginId") String loginId,
            @RequestParam("loginPw") String loginPw,
            @RequestParam("birthDate") String birthDate,
            @RequestParam("cardPassword") String cardPassword,
            @RequestParam(value="cardImage", required=false) MultipartFile cardImage,
            HttpServletRequest request) throws Exception {


        System.out.println("=== @RequestParam으로 받은 데이터 ===");
        System.out.println("organization: [" + organization + "]");
        System.out.println("loginId: [" + loginId + "]");
        System.out.println("loginPw: [" + loginPw + "]");
        System.out.println("birthDate: [" + birthDate + "]");
        System.out.println("cardPassword: [" + cardPassword + "]");
        System.out.println("cardImage: [" + (cardImage != null ? cardImage.getOriginalFilename() : "null") + "]");

        try {
            // DTO 수동 생성
            CardRegisterRequestDTO req = new CardRegisterRequestDTO();
            req.setOrganization(organization);
            req.setLoginId(loginId);
            req.setLoginPw(loginPw);
            req.setBirthDate(birthDate);
            req.setCardPassword(cardPassword);
            req.setCardImage(cardImage);

            // OCR로 카드번호 추출
            if(req.getCardImage() != null && !req.getCardImage().isEmpty()){
                String ocrCardNumber = clovaOCRService.extractCardNumber(req.getCardImage());
                req.setEncryptedCardNo(ocrCardNumber);
            }
            Long userId = jwtUtil.getUserIdFromToken(request);

            User user = userService.findById(userId);
            user.setBirthDate(req.getBirthDate());
            userService.updateUser(user);

            String connectedId =
                    cardService.registerAccount(req);

            List<Map<String,Object>> cards =
                    cardService.fetchCardList(
                            connectedId,
                            req.getOrganization(),
                            req.getBirthDate()
                    );

            cardService.saveCards(cards, connectedId, req.getOrganization(), userId, req.getEncryptedCardNo(), req.getCardPassword());

            return ResponseEntity.ok(Map.of(
                    "connectedId", connectedId,
                    "cards",       cards
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<?> getCardDetail(
            @PathVariable Long cardId,
            HttpServletRequest request
    ) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(request);

            CardDetailResponseDTO response = cardService.getCardDetailWithUsage(cardId, userId);

            return ResponseEntity.ok(Map.of("card", response));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserCardList(HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(request);
            List<CardDetailResponseDTO> cardList = cardService.getCardListByUser(userId);
            return ResponseEntity.ok(Map.of("cards", cardList));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}