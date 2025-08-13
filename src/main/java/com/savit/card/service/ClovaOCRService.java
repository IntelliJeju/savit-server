package com.savit.card.service;

import org.springframework.web.multipart.MultipartFile;

public interface ClovaOCRService {
     String extractCardNumber(MultipartFile cardImage) throws Exception;
     String callClovaOCR(MultipartFile file) throws Exception;
}
