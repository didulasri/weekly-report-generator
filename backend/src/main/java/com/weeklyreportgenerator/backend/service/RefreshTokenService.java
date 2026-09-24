package com.weeklyreportgenerator.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreportgenerator.backend.entity.RefreshToken;
import com.weeklyreportgenerator.backend.entity.User;
import com.weeklyreportgenerator.backend.exception.RefreshTokenInvalidException;
import com.weeklyreportgenerator.backend.repository.RefreshTokenRepository;
import com.weeklyreportgenerator.backend.repository.UserRepository;
import com.weeklyreportgenerator.backend.security.SecureTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int USER_AGENT_MAX_LENGTH = 255;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final SecureTokenService secureTokenService;
    private final RefreshTokenFamilyRevoker familyRevoker;

    @Value("${app.refresh-token.expiration-days}")
    private long expirationDays;

    public record IssuedToken(String rawToken, UUID familyId) {
    }

    public record RotationResult(String rawToken, User user) {
    }

    // Raw token + the persisted (hash-only) entity -- kept together only transiently inside this
    // service, never stored or passed further than needed to build the next response.
    private record Created(String rawToken, RefreshToken entity) {
    }

    // A brand new login starts a brand new family -- every token later rotated from this one
    // shares familyId, which is what lets a reuse event revoke all of them together. Takes just
    // the id (resolved to a lazy reference, no extra SELECT) since the caller -- AuthController,
    // right after authenticating -- only has CustomUserDetails, not a managed User entity.
    @Transactional
    public IssuedToken issueNewFamily(Long userId, String userAgent, String ipAddress) {
        User userRef = userRepository.getReferenceById(userId);
        Created created = createAndSave(userRef, UUID.randomUUID(), userAgent, ipAddress);
        return new IssuedToken(created.rawToken(), created.entity().getFamilyId());
    }

    // Rotation: the presented token is looked up by hash, never scanned/compared in application
    // code (an exact index match is how token lookups must work -- see SecureTokenService).
    @Transactional
    public RotationResult rotate(String presentedRawToken, String userAgent, String ipAddress) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(secureTokenService.hash(presentedRawToken))
                .orElseThrow(() -> new RefreshTokenInvalidException("Unknown refresh token"));

        if (existing.getRevokedAt() != null) {
            log.warn("Refresh token reuse detected for user {} in family {} -- revoking entire family",
                    existing.getUser().getId(), existing.getFamilyId());
            // A separate bean/transaction (REQUIRES_NEW) so this commits even though this method
            // throws right after -- otherwise Spring would roll the revocation back along with the
            // (empty) rest of this transaction, and the reuse-detected family would stay live.
            familyRevoker.revokeFamily(existing.getFamilyId());
            throw new RefreshTokenInvalidException("Refresh token reuse detected");
        }

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenInvalidException("Refresh token expired");
        }

        User user = existing.getUser();
        // Force the lazy role association to load now, inside the transaction -- the caller
        // (AuthController, building a fresh JWT from this user) runs after the transaction and
        // its persistence context are both gone.
        user.getRole().getName();
        Created next = createAndSave(user, existing.getFamilyId(), userAgent, ipAddress);

        existing.setRevokedAt(Instant.now());
        existing.setReplacedBy(next.entity());
        refreshTokenRepository.save(existing);

        return new RotationResult(next.rawToken(), user);
    }

    // A deliberate logout only revokes the one token presented -- unlike reuse detection, there is
    // no reason to suspect the rest of the family, so the other devices stay logged in.
    @Transactional
    public void revokeToken(String rawToken) {
        refreshTokenRepository.findByTokenHash(secureTokenService.hash(rawToken))
                .filter(t -> t.getRevokedAt() == null)
                .ifPresent(t -> {
                    t.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(t);
                });
    }

    // The revocation hooks: password reset, admin deactivation, admin role change.
    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    private Created createAndSave(User user, UUID familyId, String userAgent, String ipAddress) {
        String rawToken = secureTokenService.generate();
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(secureTokenService.hash(rawToken))
                .familyId(familyId)
                .expiresAt(Instant.now().plus(expirationDays, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .userAgent(truncate(userAgent))
                .ipAddress(ipAddress)
                .build();
        RefreshToken saved = refreshTokenRepository.save(token);
        return new Created(rawToken, saved);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > USER_AGENT_MAX_LENGTH ? value.substring(0, USER_AGENT_MAX_LENGTH) : value;
    }
}
