package com.savit.user.mapper;


import com.savit.user.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    User findByEmail(String email);
    User findByKakaoUserId(String kakaoUserId);
    void insertUser(User user);
    void updateUser(User user);
    User findById(Long id);
    int updateBirthDateIfNull(@Param("userId") Long userId,
                              @Param("birthDate") String birthDate);
    
    // ===== 내부 메서드 호출 방식 - 스케줄러에서 사용 =====
    
    /**
     * 내부 메서드 호출 방식 - 카드가 등록된 활성 사용자 조회
     * 카드 승인내역 동기화 스케줄러에서 사용
     */
    List<User> findUsersWithCards();
    
    /**
     * 내부 메서드 호출 방식 - FCM 토큰이 등록된 활성 사용자 조회  
     * 랜덤 잔소리 알림 스케줄러에서 사용
     */
    List<User> findUsersWithFcmTokens();
}
