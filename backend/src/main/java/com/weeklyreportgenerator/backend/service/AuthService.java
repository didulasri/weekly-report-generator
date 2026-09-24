package com.weeklyreportgenerator.backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.weeklyreportgenerator.backend.dto.request.LoginRequest;
import com.weeklyreportgenerator.backend.dto.response.AuthResponse;
import com.weeklyreportgenerator.backend.dto.response.UserSummaryResponse;
import com.weeklyreportgenerator.backend.security.CustomUserDetails;
import com.weeklyreportgenerator.backend.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    // Checked before every attempt, not just after failures -- a locked account must stay locked
    // even if the caller happens to submit the correct password mid-lockout.
    public AuthResponse login(LoginRequest request) {
        loginAttemptService.assertNotLocked(request.getEmail());

        CustomUserDetails userDetails;
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            userDetails = (CustomUserDetails) authentication.getPrincipal();
        } catch (BadCredentialsException ex) {
            loginAttemptService.onLoginFailure(request.getEmail());
            throw ex;
        }

        loginAttemptService.onLoginSuccess(request.getEmail());
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(toSummary(userDetails))
                .build();
    }

    public UserSummaryResponse getCurrentUser(CustomUserDetails userDetails) {
        return toSummary(userDetails);
    }

    private UserSummaryResponse toSummary(CustomUserDetails userDetails) {
        return UserSummaryResponse.builder()
                .id(userDetails.getId())
                .name(userDetails.getName())
                .email(userDetails.getEmail())
                .role(userDetails.getRole())
                .build();
    }
}
