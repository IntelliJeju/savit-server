package com.savit.user.service;

import com.savit.user.domain.User;
import com.savit.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    @Override
    public User findByKakaoUserId(String kakaoUserId) {
        return userMapper.findByKakaoUserId(kakaoUserId);
    }

    @Override
    public void createUser(User user) {
        userMapper.insertUser(user);
    }

    @Override
    public void updateUser(User user) {
        userMapper.updateUser(user);
    }

    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public void updateBirthDateIfNull(Long userId, String birthDate) {
        userMapper.updateBirthDateIfNull(userId, birthDate);
    }
}
