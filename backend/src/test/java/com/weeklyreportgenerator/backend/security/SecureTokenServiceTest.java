package com.weeklyreportgenerator.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class SecureTokenServiceTest {

    private final SecureTokenService service = new SecureTokenService();

    private static final Pattern BASE64_URL_NO_PADDING = Pattern.compile("^[A-Za-z0-9_-]+$");

    @Test
    void generateProducesUrlSafeTokensWithNoPadding() {
        String token = service.generate();

        assertThat(token).doesNotContain("+", "/", "=");
        assertThat(BASE64_URL_NO_PADDING.matcher(token).matches()).isTrue();
        // 32 raw bytes, base64url-encoded without padding -> ceil(32*8/6) = 43 characters.
        assertThat(token).hasSize(43);
    }

    @Test
    void generateProducesDistinctTokensEachCall() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            tokens.add(service.generate());
        }
        assertThat(tokens).hasSize(1000);
    }

    @Test
    void hashIsDeterministicAndLooksLikeSha256Hex() {
        String raw = "a-known-raw-token-value";
        String hash1 = service.hash(raw);
        String hash2 = service.hash(raw);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 -> 32 bytes -> 64 hex chars
        assertThat(hash1).matches("^[0-9a-f]{64}$");
    }

    @Test
    void differentRawTokensProduceDifferentHashes() {
        assertThat(service.hash("token-a")).isNotEqualTo(service.hash("token-b"));
    }

    @Test
    void hashesMatchComparesEqualHashesAsTrue() {
        String hash = service.hash("same-input");
        assertThat(service.hashesMatch(hash, service.hash("same-input"))).isTrue();
    }

    @Test
    void hashesMatchComparesDifferentHashesAsFalse() {
        assertThat(service.hashesMatch(service.hash("one"), service.hash("two"))).isFalse();
    }
}
