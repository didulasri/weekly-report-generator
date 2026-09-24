package com.weeklyreportgenerator.backend.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreportgenerator.backend.exception.AccountLockedException;
import com.weeklyreportgenerator.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

// Per-account lockout, tracked on the user row (not in-memory) so it survives restarts. Checked
// BEFORE authenticating on every login attempt (assertNotLocked) rather than raised the instant the
// threshold is crossed -- the attempt that trips the lock still returns the ordinary 401 for that
// wrong password, and only the *next* attempt sees the 423. Simpler state machine, and avoids the
// question of "which exception wins" on the crossing attempt.
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository userRepository;

    @Value("${app.security.lockout.max-attempts}")
    private int maxAttempts;

    @Value("${app.security.lockout.duration-minutes}")
    private long lockoutDurationMinutes;

    @Transactional(readOnly = true)
    public void assertNotLocked(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            Instant lockedUntil = user.getLockedUntil();
            if (lockedUntil != null && lockedUntil.isAfter(Instant.now())) {
                long minutesLeft = Math.max(1, Duration.between(Instant.now(), lockedUntil).toMinutes());
                throw new AccountLockedException(
                        "Account temporarily locked due to too many failed login attempts. Try again in "
                                + minutesLeft + " minute(s).");
            }
        });
    }

    @Transactional
    public void onLoginSuccess(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        });
    }

    @Transactional
    public void onLoginFailure(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= maxAttempts) {
                user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(lockoutDurationMinutes)));
            }
            userRepository.save(user);
        });
    }
}
