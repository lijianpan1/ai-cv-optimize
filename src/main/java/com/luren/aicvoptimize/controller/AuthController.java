package com.luren.aicvoptimize.controller;

import com.luren.aicvoptimize.dto.LoginRequest;
import com.luren.aicvoptimize.dto.LoginResponse;
import com.luren.aicvoptimize.dto.RegisterRequest;
import com.luren.aicvoptimize.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {
        // 这个端点用于获取当前用户信息，由 Security 保护
        return ResponseEntity.ok(Map.of("success", true));
    }
}
