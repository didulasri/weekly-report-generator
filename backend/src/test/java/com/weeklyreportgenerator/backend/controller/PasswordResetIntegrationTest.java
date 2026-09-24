package com.weeklyreportgenerator.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.weeklyreportgenerator.backend.service.EmailService;

import jakarta.servlet.http.Cookie;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class PasswordResetIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private String uniqueEmail(String prefix) {
        return prefix + "-" + COUNTER.incrementAndGet() + "-" + System.nanoTime() + "@example.com";
    }

    private RequestPostProcessor loginAndGetToken(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie accessTokenCookie = loginResult.getResponse().getCookie("access_token");
        Cookie refreshTokenCookie = loginResult.getResponse().getCookie("refresh_token");

        Cookie xsrfCookie = mockMvc.perform(get("/api/auth/csrf").cookie(accessTokenCookie, refreshTokenCookie))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        return request -> {
            request.setCookies(accessTokenCookie, refreshTokenCookie, xsrfCookie);
            request.addHeader("X-XSRF-TOKEN", xsrfCookie.getValue());
            return request;
        };
    }

    private RequestPostProcessor adminToken() throws Exception {
        return loginAndGetToken("admin@example.com", "Password123");
    }

    private Long createActiveTeamMember(String name, String email, String password) {
        Long roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = 'TEAM_MEMBER'", Long.class);
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (name, email, password, role_id, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, true, now(), now())
                RETURNING id
                """, Long.class, name, email, passwordEncoder.encode(password), roleId);
    }

    private String extractTokenFromLink(String link) {
        int idx = link.indexOf("token=");
        return link.substring(idx + "token=".length());
    }

    private String requestResetAndGetToken(String email) throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(3000)).sendPasswordResetEmail(
                eq(email), anyString(), linkCaptor.capture(), any(Instant.class));
        return extractTokenFromLink(linkCaptor.getValue());
    }

    @Test
    void forgotPasswordAlwaysReturns202WithTheSameBodyForAKnownOrUnknownEmail() throws Exception {
        String knownEmail = uniqueEmail("known");
        createActiveTeamMember("Known User", knownEmail, "Password123");

        MvcResult knownResult = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + knownEmail + "\"}"))
                .andExpect(status().isAccepted())
                .andReturn();

        MvcResult unknownResult = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + uniqueEmail("unknown") + "\"}"))
                .andExpect(status().isAccepted())
                .andReturn();

        // Same status, same body -- an attacker probing this endpoint learns nothing about which
        // emails have accounts.
        assertThat(knownResult.getResponse().getContentAsString())
                .isEqualTo(unknownResult.getResponse().getContentAsString());

        // But only the known account actually got an email.
        verify(emailService, timeout(3000)).sendPasswordResetEmail(
                eq(knownEmail), anyString(), anyString(), any(Instant.class));
    }

    @Test
    void forgotPasswordForADeactivatedAccountSendsNoEmail() throws Exception {
        String email = uniqueEmail("deactivated");
        Long id = createActiveTeamMember("Deactivated User", email, "Password123");
        RequestPostProcessor admin = adminToken();
        mockMvc.perform(delete("/api/admin/users/" + id).with(admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted());

        verify(emailService, never()).sendPasswordResetEmail(eq(email), anyString(), anyString(), any(Instant.class));
    }

    @Test
    void resetPasswordWithAValidTokenLetsUserLoginWithTheNewPasswordAndNotTheOld() throws Exception {
        String email = uniqueEmail("full-reset");
        createActiveTeamMember("Full Reset", email, "OldPassword1");
        String rawToken = requestResetAndGetToken(email);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"NewPassword2"}
                                """.formatted(rawToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"NewPassword2\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"OldPassword1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resetPasswordRevokesEveryExistingRefreshTokenForThatUser() throws Exception {
        String email = uniqueEmail("revoke-sessions");
        createActiveTeamMember("Revoke Sessions", email, "OldPassword1");

        // Establish a live session (refresh_token cookie) before the reset.
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"OldPassword1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie accessTokenCookie = loginResult.getResponse().getCookie("access_token");
        Cookie liveRefreshCookie = loginResult.getResponse().getCookie("refresh_token");
        // /api/auth/refresh is CSRF-protected (it's not in SecurityConfig's ignored-matchers list),
        // so proving the refresh token itself was revoked -- not just rejected for a missing CSRF
        // header -- means presenting a genuine XSRF-TOKEN too.
        Cookie xsrfCookie = mockMvc.perform(get("/api/auth/csrf").cookie(accessTokenCookie, liveRefreshCookie))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        String rawToken = requestResetAndGetToken(email);
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"NewPassword2"}
                                """.formatted(rawToken)))
                .andExpect(status().isNoContent());

        // The refresh token that was live before the reset must now be rejected.
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(liveRefreshCookie)
                        .cookie(xsrfCookie)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resettingWithAnAlreadyUsedTokenReturns410() throws Exception {
        String email = uniqueEmail("reuse-reset");
        createActiveTeamMember("Reuse Reset", email, "OldPassword1");
        String rawToken = requestResetAndGetToken(email);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"NewPassword2"}
                                """.formatted(rawToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"AnotherPassword3"}
                                """.formatted(rawToken)))
                .andExpect(status().isGone());
    }

    @Test
    void resettingWithAnUnknownTokenReturns410() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"not-a-real-token","newPassword":"NewPassword2"}
                                """))
                .andExpect(status().isGone());
    }

    @Test
    void requestingASecondResetInvalidatesTheFirstToken() throws Exception {
        String email = uniqueEmail("second-request");
        createActiveTeamMember("Second Request", email, "OldPassword1");
        String firstToken = requestResetAndGetToken(email);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted());
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(3000).times(2)).sendPasswordResetEmail(
                eq(email), anyString(), linkCaptor.capture(), any(Instant.class));
        String secondToken = extractTokenFromLink(linkCaptor.getValue());
        assertThat(secondToken).isNotEqualTo(firstToken);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"NewPassword2"}
                                """.formatted(firstToken)))
                .andExpect(status().isGone());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"NewPassword2"}
                                """.formatted(secondToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void resetWithAWeakPasswordReturns400() throws Exception {
        String email = uniqueEmail("weak-reset");
        createActiveTeamMember("Weak Reset", email, "OldPassword1");
        String rawToken = requestResetAndGetToken(email);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"short"}
                                """.formatted(rawToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminTriggeredResetSendsTheSameEmailFlowAsSelfService() throws Exception {
        String email = uniqueEmail("admin-triggered");
        Long id = createActiveTeamMember("Admin Triggered", email, "OldPassword1");
        RequestPostProcessor admin = adminToken();

        mockMvc.perform(post("/api/admin/users/" + id + "/send-password-reset").with(admin))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message", Matchers.containsString("password reset link")));

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(3000)).sendPasswordResetEmail(
                eq(email), anyString(), linkCaptor.capture(), any(Instant.class));
        String rawToken = extractTokenFromLink(linkCaptor.getValue());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"NewPassword2"}
                                """.formatted(rawToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonAdminCannotTriggerAnAdminPasswordReset() throws Exception {
        String email = uniqueEmail("blocked-admin-reset");
        Long id = createActiveTeamMember("Blocked", email, "OldPassword1");
        RequestPostProcessor member = loginAndGetToken("member@example.com", "Password123");

        mockMvc.perform(post("/api/admin/users/" + id + "/send-password-reset").with(member))
                .andExpect(status().isForbidden());
    }
}
