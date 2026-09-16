package com.weeklyreportgenerator.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreportgenerator.backend.dto.request.LoginRequest;
import com.weeklyreportgenerator.backend.dto.request.RegisterRequest;
import com.weeklyreportgenerator.backend.dto.response.AuthResponse;
import com.weeklyreportgenerator.backend.dto.response.UserSummaryResponse;
import com.weeklyreportgenerator.backend.security.CustomUserDetails;
import com.weeklyreportgenerator.backend.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummaryResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal));
    }
}
