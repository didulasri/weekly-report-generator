package com.weeklyreportgenerator.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreportgenerator.backend.entity.PasswordResetToken;
import com.weeklyreportgenerator.backend.entity.User;
import com.weeklyreportgenerator.backend.event.PasswordResetRequestedEvent;
import com.weeklyreportgenerator.backend.exception.PasswordResetTokenInvalidException;
import com.weeklyreportgenerator.backend.exception.ResourceNotFoundException;
import com.weeklyreportgenerator.backend.repository.PasswordResetTokenRepository;
import com.weeklyreportgenerator.backend.repository.UserRepository;
import com.weeklyreportgenerator.backend.security.SecureTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenService secureTokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final RefreshTokenService refreshTokenService;

    // Deliberately silent on a missing or inactive account -- the controller always answers 202
    // with the same body regardless of what happens here, so this endpoint can't be used to
    // discover which emails have accounts.
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email)
                .filter(User::isActive)
                .ifPresent(this::initiateReset);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void adminRequestReset(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        // A deactivated account can't log in regardless -- don't hand it a working reset link.
        if (user.isActive()) {
            initiateReset(user);
        }
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(secureTokenService.hash(rawToken))
                .filter(this::isUsable)
                .orElseThrow(PasswordResetTokenInvalidException::new);

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);

        // Logs out every device -- a password reset means the old credential (and anything an
        // attacker who had it could still be holding) must stop working immediately, not linger
        // for up to 7 days on other sessions.
        refreshTokenService.revokeAllForUser(user.getId());
    }

    private void initiateReset(User user) {
        Instant now = Instant.now();
        tokenRepository.invalidateUnusedForUser(user.getId(), now);

        String rawToken = secureTokenService.generate();
        Instant expiresAt = now.plus(EXPIRY_MINUTES, ChronoUnit.MINUTES);

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash(secureTokenService.hash(rawToken))
                .expiresAt(expiresAt)
                .createdAt(now)
                .build();
        tokenRepository.save(token);

        eventPublisher.publishEvent(
                new PasswordResetRequestedEvent(user.getEmail(), user.getName(), rawToken, expiresAt));
    }

    private boolean isUsable(PasswordResetToken token) {
        return token.getUsedAt() == null && token.getExpiresAt().isAfter(Instant.now());
    }
}
