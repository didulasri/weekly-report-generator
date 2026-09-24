package com.weeklyreportgenerator.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

// Single source of truth for every single-use token in the system: invitations, password resets,
// and refresh tokens. The raw token is only ever handed to the caller (to email or set as a
// cookie) -- only its SHA-256 hash is stored, so a database read can never leak a usable token.
@Component
public class SecureTokenService {

    private static final int TOKEN_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a mandatory JDK algorithm (JLS/JCA spec) -- this cannot happen at runtime.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    // Constant-time comparison so a timing side-channel can't be used to guess a valid hash one
    // byte at a time. Callers should still look tokens up by hash (an exact index match), not
    // scan-and-compare -- this exists for the rare spot where two known hashes must be compared.
    public boolean hashesMatch(String hashA, String hashB) {
        return MessageDigest.isEqual(
                hashA.getBytes(StandardCharsets.UTF_8),
                hashB.getBytes(StandardCharsets.UTF_8));
    }
}
