package com.weeklyreportgenerator.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.weeklyreportgenerator.backend.entity.User;
import com.weeklyreportgenerator.backend.exception.AccountLockedException;
import com.weeklyreportgenerator.backend.repository.UserRepository;

// No Spring context and no Docker needed -- UserRepository is mocked, and the @Value-injected
// thresholds are set directly via ReflectionTestUtils the same way SmtpEmailServiceTest sets
// fromAddress. This runs (and actually proves something) even while Testcontainers can't.
class LoginAttemptServiceTest {

    private static final String EMAIL = "locked-candidate@example.com";
    private static final int MAX_ATTEMPTS = 5;
    private static final long DURATION_MINUTES = 15;

    private UserRepository userRepository;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new LoginAttemptService(userRepository);
        ReflectionTestUtils.setField(service, "maxAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(service, "lockoutDurationMinutes", DURATION_MINUTES);
    }

    private User freshUser() {
        User user = User.builder().email(EMAIL).failedLoginAttempts(0).lockedUntil(null).build();
        user.setId(1L);
        return user;
    }

    @Test
    void assertNotLockedDoesNothingForAnUnknownEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.assertNotLocked(EMAIL);

        // Never touched -- an unknown email can't be "locked", and this must not leak whether the
        // account exists via a different exception/behaviour than a known, unlocked account.
        verify(userRepository, never()).save(any());
    }

    @Test
    void assertNotLockedDoesNothingWhenNeverLocked() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(freshUser()));

        service.assertNotLocked(EMAIL);
    }

    @Test
    void assertNotLockedThrowsWhileLockedUntilIsInTheFuture() {
        User user = freshUser();
        user.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.assertNotLocked(EMAIL))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void assertNotLockedDoesNothingOncePastLockedUntil() {
        User user = freshUser();
        user.setLockedUntil(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.assertNotLocked(EMAIL);
    }

    @Test
    void failuresBelowTheThresholdIncrementCountAndDoNotLock() {
        User user = freshUser();
        user.setFailedLoginAttempts(MAX_ATTEMPTS - 2);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.onLoginFailure(EMAIL);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(MAX_ATTEMPTS - 1);
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void theFailureThatReachesMaxAttemptsSetsLockedUntilInTheFuture() {
        User user = freshUser();
        user.setFailedLoginAttempts(MAX_ATTEMPTS - 1);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.onLoginFailure(EMAIL);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(user.getLockedUntil()).isAfter(Instant.now());
        assertThat(user.getLockedUntil()).isBeforeOrEqualTo(Instant.now().plus(DURATION_MINUTES, ChronoUnit.MINUTES));
    }

    @Test
    void aFailureAfterAlreadyBeingLockedExtendsTheLockRatherThanErroring() {
        User user = freshUser();
        user.setFailedLoginAttempts(MAX_ATTEMPTS);
        Instant previousLock = Instant.now().plus(1, ChronoUnit.MINUTES);
        user.setLockedUntil(previousLock);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.onLoginFailure(EMAIL);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(MAX_ATTEMPTS + 1);
        assertThat(user.getLockedUntil()).isAfter(previousLock);
    }

    @Test
    void onLoginSuccessResetsCountAndClearsLock() {
        User user = freshUser();
        user.setFailedLoginAttempts(3);
        user.setLockedUntil(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.onLoginSuccess(EMAIL);

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void onLoginSuccessDoesNotWriteWhenThereWasNothingToReset() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(freshUser()));

        service.onLoginSuccess(EMAIL);

        verify(userRepository, never()).save(any());
    }

    @Test
    void onLoginFailureForAnUnknownEmailDoesNotThrowOrWrite() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.onLoginFailure(EMAIL);

        verify(userRepository, never()).save(any());
    }
}
