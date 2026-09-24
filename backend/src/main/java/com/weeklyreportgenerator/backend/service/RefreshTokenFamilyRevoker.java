package com.weeklyreportgenerator.backend.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreportgenerator.backend.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

// A separate bean, not just a private method on RefreshTokenService, because
// REQUIRES_NEW only takes effect through the Spring proxy -- a self-invoked call (this.method())
// from within the same class bypasses the proxy entirely and the propagation would be silently
// ignored. This exists so the family revocation survives and commits even though the caller
// (RefreshTokenService.rotate) immediately throws afterward, which would otherwise roll the
// revocation back along with everything else in that transaction.
@Service
@RequiredArgsConstructor
public class RefreshTokenFamilyRevoker {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeFamily(UUID familyId) {
        refreshTokenRepository.revokeFamily(familyId, Instant.now());
    }
}
