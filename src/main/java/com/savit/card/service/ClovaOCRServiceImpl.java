package com.savit.card.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpHeaders;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClovaOCRServiceImpl implements ClovaOCRService {
    @Value("${clova.ocr.api.url}")
    private String apiUrl;

    @Value("${clova.ocr.secret.key}")
    private String secretKey;


    @Override
    public String extractCardNumber(MultipartFile cardImage) throws Exception {
        String response = callClovaOCR(cardImage);

        //Json 파싱
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> result = mapper.readValue(response, Map.class);

        List<Map<String, Object>> images = (List<Map<String,Object>>) result.get("images");
        Map<String, Object> image =images.get(0);

        // 1. inferResult 체크
        if (!"SUCCESS".equals(image.get("inferResult"))) {
            throw new RuntimeException("OCR 인식 실패 : "+ image.get("message"));
        }

        // 2. confidence 체크
        Map<String, Object> creditCard = (Map<String, Object>) image.get("creditCard");
        Map<String, Object> cardResult = (Map<String, Object>) creditCard.get("result");
        Map<String, Object> number = (Map<String, Object>) cardResult.get("number");

        Double confidence = (Double) number.get("confidence");
        if(confidence<0.9)  { // 90% 이상만 받도록
            throw new RuntimeException("카드번호 인식 신뢰도가 낮습니다: " + confidence);
        }

        // 카드 번호 반환
        return (String) number.get("text");


    }
    @Override
    public String callClovaOCR(MultipartFile file) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-OCR-SECRET", secretKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // 메시지 Json 구성
        String message = String.format(
                "{\"version\": \"V2\", \"requestId\": \"%s\", \"timestamp\": %d, \"images\": [{\"format\": \"jpg\", \"name\": \"card\"}]}",
                UUID.randomUUID().toString(), System.currentTimeMillis()
        );
        // 요청 바디 구성 (multipart/form-data)
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("message", message);
        body.add("file", file.getResource());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // POST 요청 전송
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, requestEntity, String.class);

        return response.getBody();


    }
}
