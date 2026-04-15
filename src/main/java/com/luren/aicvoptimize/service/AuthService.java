package com.luren.aicvoptimize.service;

import com.luren.aicvoptimize.constant.RoleConstant;
import com.luren.aicvoptimize.dto.LoginRequest;
import com.luren.aicvoptimize.dto.LoginResponse;
import com.luren.aicvoptimize.dto.RegisterRequest;
import com.luren.aicvoptimize.entity.CvUser;
import com.luren.aicvoptimize.repository.UserRepository;
import com.luren.aicvoptimize.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        CvUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname()
        );
    }

    /**
     * 用户注册
     */
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        CvUser user = new CvUser();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setQuota(0);
        user.setRole(RoleConstant.USER.name());

        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname()
        );
    }
}
