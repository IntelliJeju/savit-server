package com.savit.challenge.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class IamportProperties {
    @Value("${iamport.api-key}")
    private String apiKey;

    @Value("${iamport.api-secret}")
    private String apiSecret;

    @Value("${iamport.api.base-url:https://api.iamport.kr}")
    private String baseUrl;
}