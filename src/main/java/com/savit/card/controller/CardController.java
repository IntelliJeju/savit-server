package com.savit.card.controller;

import com.savit.card.dto.CardDetailResponseDTO;
import com.savit.card.dto.CardRegisterRequestDTO;
import com.savit.card.dto.CardRenameRequestDTO;
import com.savit.card.service.CardService;
import com.savit.security.JwtUtil;
import com.savit.user.domain.User;
import com.savit.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


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

    @PostMapping("/register")
    public ResponseEntity<?> registerCardAndFetch(
            @RequestBody @Valid CardRegisterRequestDTO req, HttpServletRequest request) {

        try {
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
}