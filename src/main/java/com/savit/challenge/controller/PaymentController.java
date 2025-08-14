package com.savit.challenge.controller;

import com.savit.challenge.dto.InitPaymentRequestDTO;
import com.savit.challenge.dto.InitPaymentResponseDTO;
import com.savit.challenge.dto.PaymentStatusResponseDTO;
import com.savit.challenge.service.PaymentService;
import com.savit.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final JwtUtil jwtUtil;
    private final PaymentService paymentService;

    // 1) 결제 초기화: 서버가 금액/주문 확정 + prepare (서비스 내부에서 수행)
    @PostMapping("/init")
    public ResponseEntity<InitPaymentResponseDTO> init(@RequestBody InitPaymentRequestDTO req,
                                                       HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request);
        InitPaymentResponseDTO resp = paymentService.initPayment(userId, req); // 내부에서 prepare 완료
        return ResponseEntity.ok(resp);
    }

    // 2) 상태 조회: 프론트 폴링/최종 확인
    @GetMapping("/status")
    public ResponseEntity<PaymentStatusResponseDTO> status(@RequestParam String merchantUid) {
        return ResponseEntity.ok(paymentService.getStatus(merchantUid));
    }

    // 3) 폴백 검증: 웹훅이 지연/실패한 경우 프론트에서 호출
    @PostMapping("/verify")
    public ResponseEntity<String> verify(@RequestBody Map<String, String> body,
                                         HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request);
        paymentService.verifyAndConfirm(userId, body.get("merchantUid"), body.get("impUid"));
        return ResponseEntity.ok("verified");
    }

    // 4) 웹훅: 아임포트가 호출
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody Map<String, Object> payload) {
        paymentService.verifyAndConfirm(null, (String) payload.get("merchant_uid"), (String) payload.get("imp_uid"));
        return ResponseEntity.ok("success");
    }
}
