package com.savit.user.service;

import com.savit.user.domain.User;

public interface UserService {
    User findByEmail(String email);
    User findByKakaoUserId(String kakaoUserId);
    void createUser(User user);
    void updateUser(User user);
    User findById(Long id);
    void updateBirthDateIfNull(Long userId, String birthDate);
}