package com.weeklyreportgenerator.backend.security;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

// Every cookie the app ever sets is built here, from config -- never a literal Set-Cookie string
// scattered across controllers.
@Component
public class AuthCookieService {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @Value("${app.jwt.expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.refresh-token.expiration-days}")
    private long refreshTokenExpirationDays;

    // Off by default in dev (plain HTTP on localhost) -- must be true in any real deployment,
    // which always serves over HTTPS.
    @Value("${app.cookie.secure}")
    private boolean secureCookies;

    public ResponseCookie buildAccessTokenCookie(String jwt) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, jwt)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .path("/api")
                .maxAge(Duration.ofMillis(accessTokenExpirationMs))
                .build();
    }

    // Narrow path: the browser only ever sends this cookie to /api/auth/** requests, not to every
    // request under /api -- there's no reason for e.g. /api/reports to see the refresh token at
    // all, so it doesn't.
    public ResponseCookie buildRefreshTokenCookie(String rawToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, rawToken)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ofDays(refreshTokenExpirationDays))
                .build();
    }

    public ResponseCookie clearedAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true).secure(secureCookies).sameSite("Lax").path("/api").maxAge(0).build();
    }

    public ResponseCookie clearedRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true).secure(secureCookies).sameSite("Strict").path("/api/auth").maxAge(0).build();
    }

    public long refreshTokenExpirationDays() {
        return refreshTokenExpirationDays;
    }
}
