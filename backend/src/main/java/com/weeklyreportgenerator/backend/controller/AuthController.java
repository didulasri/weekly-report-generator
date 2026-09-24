package com.weeklyreportgenerator.backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreportgenerator.backend.dto.request.ForgotPasswordRequest;
import com.weeklyreportgenerator.backend.dto.request.LoginRequest;
import com.weeklyreportgenerator.backend.dto.request.ResetPasswordRequest;
import com.weeklyreportgenerator.backend.dto.response.AuthResponse;
import com.weeklyreportgenerator.backend.dto.response.MessageResponse;
import com.weeklyreportgenerator.backend.dto.response.UserSummaryResponse;
import com.weeklyreportgenerator.backend.exception.RefreshTokenInvalidException;
import com.weeklyreportgenerator.backend.security.AuthCookieService;
import com.weeklyreportgenerator.backend.security.CustomUserDetails;
import com.weeklyreportgenerator.backend.security.JwtService;
import com.weeklyreportgenerator.backend.service.AuthService;
import com.weeklyreportgenerator.backend.service.PasswordResetService;
import com.weeklyreportgenerator.backend.service.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// The browser never sees a token in a response body -- both the access and refresh tokens travel
// only as httpOnly cookies, built centrally by AuthCookieService. There is exactly one way in
// (the access_token cookie, read by JwtAuthenticationFilter); Authorization headers are ignored.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final MessageResponse FORGOT_PASSWORD_ACK = MessageResponse.builder()
            .message("If an account with that email exists, a password reset link has been sent.")
            .build();

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieService authCookieService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<UserSummaryResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        AuthResponse authResult = authService.login(request);

        RefreshTokenService.IssuedToken refreshToken = refreshTokenService.issueNewFamily(
                authResult.getUser().getId(), servletRequest.getHeader("User-Agent"), servletRequest.getRemoteAddr());

        ResponseCookie accessCookie = authCookieService.buildAccessTokenCookie(authResult.getAccessToken());
        ResponseCookie refreshCookie = authCookieService.buildRefreshTokenCookie(refreshToken.rawToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(authResult.getUser());
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = AuthCookieService.REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
            HttpServletRequest servletRequest) {
        if (refreshTokenCookie == null) {
            throw new RefreshTokenInvalidException("No refresh token presented");
        }

        RefreshTokenService.RotationResult result = refreshTokenService.rotate(
                refreshTokenCookie, servletRequest.getHeader("User-Agent"), servletRequest.getRemoteAddr());

        String newAccessToken = jwtService.generateToken(new CustomUserDetails(result.user()));
        ResponseCookie accessCookie = authCookieService.buildAccessTokenCookie(newAccessToken);
        ResponseCookie refreshCookie = authCookieService.buildRefreshTokenCookie(result.rawToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookieService.REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie) {
        if (refreshTokenCookie != null) {
            refreshTokenService.revokeToken(refreshTokenCookie);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.clearedAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authCookieService.clearedRefreshTokenCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummaryResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal));
    }

    // The SPA calls this once on first load to receive the XSRF-TOKEN cookie -- Spring Security's
    // CsrfToken is deferred (not actually generated/saved until something reads .getToken()), so
    // simply declaring the parameter is not enough.
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.getEmail());
        return FORGOT_PASSWORD_ACK;
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
    }
}
