package com.savit.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Admin SDK 설정
 * 공식 문서: https://firebase.google.com/docs/cloud-messaging/server
 */
@Slf4j
@Configuration
public class FirebaseConfig  {

    @Value("${firebase.config.path}")
    private String firebaseConfigPath;

    /**
     * Firebase App 초기화
     * Firebase Admin SDK의 진입점
     */
    @Bean
    public FirebaseApp firebaseApp() {
        try {
            if(FirebaseApp.getApps().isEmpty()) {
                InputStream inputStream;

                // 외부 마운트된 파일 경로로 시도
                java.io.File externalFile = new java.io.File(firebaseConfigPath);
                if (externalFile.exists()) {
                    log.info("외부 Firebase 설정 파일 사용: {}", firebaseConfigPath);
                    inputStream = new java.io.FileInputStream(externalFile);
                } else {
                    log.info("클래스패스 Firebase 설정 파일 사용: {}", firebaseConfigPath);
                    inputStream = new ClassPathResource(firebaseConfigPath).getInputStream();
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(inputStream))
                        .build();

                log.info("firebase app 초기화 완료");
                return FirebaseApp.initializeApp(options);
            } else {
                // 이미 파베 앱 만들어졌으면 기존의 디폴트 앱 가져와서 반환
                return FirebaseApp.getInstance();
            }

        } catch (IOException exception) {
            log.error("firebase app 초기화 실패 {}", exception.getMessage());
            return null;
        }
    }

    /**
     * FirebaseMessaging Bean 생성
     * FCM 메시지 전송을 위한 인스턴스임!
     */
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        try {
            return FirebaseMessaging.getInstance(firebaseApp);
        } catch (Exception e) {
            log.warn("FirebaseMessaging 인스턴스 생성 실패: {}", e.getMessage());
            return null; // null을 반환하여 서비스에서 체크할 수 있도록 함
        }
    }

}