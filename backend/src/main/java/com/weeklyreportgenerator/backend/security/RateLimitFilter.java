package com.weeklyreportgenerator.backend.security;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.weeklyreportgenerator.backend.dto.response.ErrorResponse;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

// Per-IP request throttling on the two unauthenticated endpoints that can be hammered to guess a
// password or exhaust the mail queue: login and forgot-password. This is deliberately in-memory
// (a single ConcurrentHashMap<String, Bucket>, one bucket per IP+endpoint) since the app runs as a
// single instance -- a multi-instance deployment would need a shared store (e.g. Redis) instead,
// but that's not this app's problem yet. This is a coarse IP-level throttle; it is not a substitute
// for the per-account lockout in LoginAttemptService, which is what actually stops a distributed
// guessing attack against one specific account.
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String FORGOT_PASSWORD_PATH = "/api/auth/forgot-password";

    private final ObjectMapper objectMapper;

    @Value("${app.security.rate-limit.login-per-minute}")
    private int loginPerMinute;

    @Value("${app.security.rate-limit.forgot-password-per-minute}")
    private int forgotPasswordPerMinute;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean isLogin = "POST".equalsIgnoreCase(request.getMethod()) && path.equals(LOGIN_PATH);
        boolean isForgotPassword = "POST".equalsIgnoreCase(request.getMethod()) && path.equals(FORGOT_PASSWORD_PATH);

        if (!isLogin && !isForgotPassword) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit = isLogin ? loginPerMinute : forgotPasswordPerMinute;
        String key = path + ":" + clientIp(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(limit));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("TOO_MANY_REQUESTS")
                .message("Too many requests -- please try again shortly")
                .path(path)
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "60");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private Bucket newBucket(int permitsPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(permitsPerMinute)
                .refillGreedy(permitsPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
