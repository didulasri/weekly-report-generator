package com.weeklyreportgenerator.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.weeklyreportgenerator.backend.entity.Role;
import com.weeklyreportgenerator.backend.entity.User;
import com.weeklyreportgenerator.backend.entity.enums.RoleName;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;

class JwtServiceTest {

    private static final String SECRET = "test-only-secret-key-for-jwt-service-unit-tests-32bytes-min";

    private JwtService jwtService;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000L);

        Role role = Role.builder().name(RoleName.TEAM_MEMBER).build();
        User user = User.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password("hashed")
                .role(role)
                .active(true)
                .build();
        user.setId(42L);
        userDetails = new CustomUserDetails(user);
    }

    @Test
    void generatesAndValidatesTokenRoundTrip() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractEmail(token)).isEqualTo("jane@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("TEAM_MEMBER");
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        JwtService shortLivedJwtService = new JwtService(SECRET, 1L);
        String token = shortLivedJwtService.generateToken(userDetails);

        Thread.sleep(20);

        assertThat(shortLivedJwtService.isTokenValid(token, userDetails)).isFalse();
        assertThrows(ExpiredJwtException.class, () -> shortLivedJwtService.extractEmail(token));
    }

    @Test
    void tamperedSignatureIsRejected() {
        String token = jwtService.generateToken(userDetails);
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThat(jwtService.isTokenValid(tampered, userDetails)).isFalse();
        assertThrows(SignatureException.class, () -> jwtService.extractEmail(tampered));
    }

    @Test
    void malformedTokenIsRejectedByIsTokenValid() {
        assertThat(jwtService.isTokenValid("not-a-jwt", userDetails)).isFalse();
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        JwtService otherJwtService = new JwtService("a-completely-different-secret-key-for-this-test-32bytes+", 60_000L);
        String token = otherJwtService.generateToken(userDetails);

        assertThrows(JwtException.class, () -> jwtService.extractEmail(token));
    }
}
