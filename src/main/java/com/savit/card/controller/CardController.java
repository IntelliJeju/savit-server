package com.savit.card.controller;

import com.savit.card.dto.CardDetailResponseDTO;
import com.savit.card.dto.CardRegisterRequestDTO;
import com.savit.card.dto.CardRenameRequestDTO;
import com.savit.card.service.CardService;
import com.savit.card.service.ClovaOCRService;
import com.savit.security.JwtUtil;
import com.savit.user.domain.User;
import com.savit.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CardController {

    private final CardService cardService;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final ClovaOCRService clovaOCRService;

    // OCR 로 카드 번호 추출
    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> extractCardNumber(@RequestParam("cardImage") MultipartFile cardImage) {
        try {
            if (cardImage == null || cardImage.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "카드 이미지가 필요합니다."));
            }

            String ocrCardNumber = clovaOCRService.extractCardNumber(cardImage);

            return ResponseEntity.ok(Map.of(
                    "cardNumber", ocrCardNumber,
                    "message", "카드번호가 성공적으로 인식되었습니다."
            ));
        } catch (Exception e) {
            log.error("OCR 카드번호 추출 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "카드번호 인식에 실패했습니다: " + e.getMessage()));
        }
    }

    // 카드 등록
    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> registerCardAndFetch(
            @RequestBody @Valid CardRegisterRequestDTO req,
            HttpServletRequest request) {


        try {
            Long userId = jwtUtil.getUserIdFromToken(request);

            // 카드번호가 없으면 에러
            if (req.getEncryptedCardNo() == null || req.getEncryptedCardNo().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "카드번호가 필요합니다. 먼저 OCR로 카드번호를 인식해주세요."));
            }

            String connectedId =
                    cardService.registerAccount(req);

            // 생년월일 업데이트
            String birthForDb = toYyyyDashMmDashDd(req.getBirthDate());
            if (birthForDb != null && !birthForDb.isBlank()) {
                userService.updateBirthDateIfNull(userId, birthForDb);
            }

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

    @PatchMapping("/{cardId}/name")
    public ResponseEntity<?> renameCard(
            @PathVariable Long cardId,
            @RequestBody @Valid CardRenameRequestDTO req,
            HttpServletRequest request
    ) {
        Long userId = jwtUtil.getUserIdFromToken(request);

        var updated = cardService.renameCardAndFetch(cardId, userId, req.getCardName());
        if (updated == null) {
            return ResponseEntity.status(404).body(Map.of("error", "not found"));
        }

        return ResponseEntity.ok(Map.of(
                "cardId", updated.getId(),
                "cardName", updated.getCardName(),
                "updatedAt", updated.getUpdatedAt()
        ));
    }

    private String toYyyyDashMmDashDd(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.matches("\\d{8}")) {
            return s.substring(0,4) + "-" + s.substring(4,6) + "-" + s.substring(6,8);
        }
        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) return s;
        return s;
    }
}