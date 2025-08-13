package com.savit.user.controller;

import com.savit.security.JwtUtil;
import com.savit.user.domain.User;
import com.savit.user.dto.UserInfoDTO;
import com.savit.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping("/profile")
    public ResponseEntity<UserInfoDTO>getUserProfile(HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request);
        User user = userService.findById(userId);
        UserInfoDTO userDto = UserInfoDTO.from(user);
        return ResponseEntity.ok(userDto);
    }
}