package com.weeklyreportgenerator.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.jayway.jsonpath.JsonPath;
import com.weeklyreportgenerator.backend.service.EmailService;

import jakarta.servlet.http.Cookie;

// There is no public registration -- an account can only come to exist by an admin creating an
// invitation and the recipient accepting it. EmailService is mocked so the raw token (never
// persisted -- only its SHA-256 hash is stored) can be captured off the link the app would have
// mailed out, the same way a real recipient would get it by clicking the email link.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class InvitationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private MockMvc mockMvc;

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

    private RequestPostProcessor memberToken() throws Exception {
        return loginAndGetToken("member@example.com", "Password123");
    }

    private String extractTokenFromLink(String link) {
        int idx = link.indexOf("token=");
        return link.substring(idx + "token=".length());
    }

    // The pending-invitations list accumulates across every test in this class (the Testcontainers
    // Postgres instance is shared for the whole class run, not rolled back per test), so finding
    // "the" id by email requires an actual JSON-aware lookup, not a naive first-match substring
    // search -- there can be several pending invitations in the same response.
    private Long invitationIdByEmail(RequestPostProcessor admin, String email) throws Exception {
        String listResponse = mockMvc.perform(get("/api/admin/invitations?status=PENDING&size=200")
                        .with(admin))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(listResponse, "$.content[?(@.email=='" + email + "')].id[0]");
        return id.longValue();
    }

    // Creates an invitation as admin and returns the raw token that would have been emailed.
    private String createInvitationAndGetToken(RequestPostProcessor admin, String email, String role) throws Exception {
        mockMvc.perform(post("/api/admin/invitations")
                        .with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"role\":\"%s\"}".formatted(email, role)))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(3000)).sendInvitationEmail(
                eq(email), eq(role), anyString(), linkCaptor.capture(), any(Instant.class));
        return extractTokenFromLink(linkCaptor.getValue());
    }

    @Test
    void nonAdminCannotCreateInvitation() throws Exception {
        mockMvc.perform(post("/api/admin/invitations")
                        .with(memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + uniqueEmail("blocked") + "\",\"role\":\"TEAM_MEMBER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotCreateInvitation() throws Exception {
        mockMvc.perform(post("/api/admin/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + uniqueEmail("blocked") + "\",\"role\":\"TEAM_MEMBER\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void creatingInvitationForExistingActiveAccountReturns409() throws Exception {
        mockMvc.perform(post("/api/admin/invitations")
                        .with(adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"member@example.com\",\"role\":\"TEAM_MEMBER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(409)));
    }

    @Test
    void creatingASecondPendingInvitationForTheSameEmailReturns409() throws Exception {
        RequestPostProcessor admin = adminToken();
        String email = uniqueEmail("double-invite");
        createInvitationAndGetToken(admin, email, "TEAM_MEMBER");

        mockMvc.perform(post("/api/admin/invitations")
                        .with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"role\":\"TEAM_MEMBER\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void fullInvitationLifecycleValidateThenAcceptLogsInAsTheInvitedRole() throws Exception {
        RequestPostProcessor admin = adminToken();
        String email = uniqueEmail("full-cycle");
        String rawToken = createInvitationAndGetToken(admin, email, "MANAGER");

        // Validate is anonymous and CSRF-exempt (permitAll + ignoringRequestMatchers).
        mockMvc.perform(post("/api/invitations/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", org.hamcrest.Matchers.is(email)))
                .andExpect(jsonPath("$.role", org.hamcrest.Matchers.is("MANAGER")));

        mockMvc.perform(post("/api/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","name":"Full Cycle","password":"Password123"}
                                """.formatted(rawToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", org.hamcrest.Matchers.is(email)))
                .andExpect(jsonPath("$.role", org.hamcrest.Matchers.is("MANAGER")));

        // The new account can now log in and reach a manager-only route.
        RequestPostProcessor newAccountToken = loginAndGetToken(email, "Password123");
        mockMvc.perform(get("/api/test/manager-only").with(newAccountToken))
                .andExpect(status().isOk());
    }

    @Test
    void acceptingAnAlreadyAcceptedTokenReturns410() throws Exception {
        RequestPostProcessor admin = adminToken();
        String email = uniqueEmail("reuse-token");
        String rawToken = createInvitationAndGetToken(admin, email, "TEAM_MEMBER");

        mockMvc.perform(post("/api/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","name":"Reuse Token","password":"Password123"}
                                """.formatted(rawToken)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","name":"Someone Else","password":"Password123"}
                                """.formatted(rawToken)))
                .andExpect(status().isGone());
    }

    @Test
    void unknownTokenReturns410OnValidateAndOnAccept() throws Exception {
        mockMvc.perform(post("/api/invitations/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"not-a-real-token\"}"))
                .andExpect(status().isGone());

        mockMvc.perform(post("/api/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"not-a-real-token","name":"Nobody","password":"Password123"}
                                """))
                .andExpect(status().isGone());
    }

    @Test
    void acceptWithAWeakPasswordReturns400AndDoesNotCreateAnAccount() throws Exception {
        RequestPostProcessor admin = adminToken();
        String email = uniqueEmail("weak-pw");
        String rawToken = createInvitationAndGetToken(admin, email, "TEAM_MEMBER");

        mockMvc.perform(post("/api/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","name":"Weak Pw","password":"short"}
                                """.formatted(rawToken)))
                .andExpect(status().isBadRequest());

        // Token is still usable -- the rejected attempt must not have consumed it.
        mockMvc.perform(post("/api/invitations/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void resendRotatesTheTokenSoTheOldLinkStopsWorking() throws Exception {
        RequestPostProcessor admin = adminToken();
        String email = uniqueEmail("resend");
        String oldToken = createInvitationAndGetToken(admin, email, "TEAM_MEMBER");

        Long invitationId = invitationIdByEmail(admin, email);

        mockMvc.perform(post("/api/admin/invitations/" + invitationId + "/resend")
                        .with(admin))
                .andExpect(status().isOk());

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(3000).times(2)).sendInvitationEmail(
                eq(email), anyString(), anyString(), linkCaptor.capture(), any(Instant.class));
        String newToken = extractTokenFromLink(linkCaptor.getValue());
        assertThat(newToken).isNotEqualTo(oldToken);

        mockMvc.perform(post("/api/invitations/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + oldToken + "\"}"))
                .andExpect(status().isGone());

        mockMvc.perform(post("/api/invitations/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + newToken + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void revokeMakesTheInvitationUnacceptable() throws Exception {
        RequestPostProcessor admin = adminToken();
        String email = uniqueEmail("revoke");
        String rawToken = createInvitationAndGetToken(admin, email, "TEAM_MEMBER");

        Long invitationId = invitationIdByEmail(admin, email);

        mockMvc.perform(delete("/api/admin/invitations/" + invitationId).with(admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","name":"Revoked","password":"Password123"}
                                """.formatted(rawToken)))
                .andExpect(status().isGone());
    }

    @Test
    void nonAdminCannotListInvitations() throws Exception {
        mockMvc.perform(get("/api/admin/invitations").with(memberToken()))
                .andExpect(status().isForbidden());
    }
}
