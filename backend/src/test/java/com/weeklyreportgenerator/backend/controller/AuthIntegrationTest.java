package com.weeklyreportgenerator.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import jakarta.servlet.http.Cookie;

// There is no public registration any more -- accounts exist only through the admin invitation
// flow (see InvitationIntegrationTest) or the bootstrap admin. Fixture accounts here are created
// with a direct JDBC insert as a stand-in for "an account exists", since this file's job is to
// test login/me, not account creation.
//
// Auth is cookie-based now: login sets httpOnly access_token/refresh_token cookies and returns
// only the user summary in the body -- there is no accessToken field to read any more.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

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

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private String loginBody(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private String createActiveTeamMember(String name, String email, String password) {
        Long roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = 'TEAM_MEMBER'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO users (name, email, password, role_id, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, true, now(), now())
                """, name, email, passwordEncoder.encode(password), roleId);
        return email;
    }

    // Logs in (cookies, not a Bearer token) and returns a RequestPostProcessor bundling the
    // access_token + refresh_token cookies plus the X-XSRF-TOKEN header -- apply with .with(...).
    private RequestPostProcessor loginAndGetToken(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
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

    @Test
    void loginWithCorrectCredentialsSetsCookiesAndReturnsUserSummary() throws Exception {
        String email = uniqueEmail("login-ok");
        createActiveTeamMember("Login User", email, "Password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "Password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.role", is("TEAM_MEMBER")))
                .andReturn();

        // No token anywhere in the body -- both tokens travel only as httpOnly cookies.
        assertThat(result.getResponse().getContentAsString().toLowerCase()).doesNotContain("token");

        Cookie accessTokenCookie = result.getResponse().getCookie("access_token");
        Cookie refreshTokenCookie = result.getResponse().getCookie("refresh_token");
        assertThat(accessTokenCookie).isNotNull();
        assertThat(accessTokenCookie.isHttpOnly()).isTrue();
        assertThat(refreshTokenCookie).isNotNull();
        assertThat(refreshTokenCookie.isHttpOnly()).isTrue();
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        String email = uniqueEmail("login-wrong-pw");
        createActiveTeamMember("Login User", email, "Password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "WrongPassword1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    void meWithoutCookieReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void meWithValidCookieReturnsCorrectUser() throws Exception {
        String email = uniqueEmail("me-ok");
        createActiveTeamMember("Me User", email, "Password123");
        RequestPostProcessor auth = loginAndGetToken(email, "Password123");

        mockMvc.perform(get("/api/auth/me").with(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.role", is("TEAM_MEMBER")));
    }

    @Test
    void teamMemberTokenIsForbiddenOnManagerOnlyRoute() throws Exception {
        String email = uniqueEmail("rbac-member");
        createActiveTeamMember("RBAC Member", email, "Password123");
        RequestPostProcessor auth = loginAndGetToken(email, "Password123");

        mockMvc.perform(get("/api/test/manager-only").with(auth))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    void managerTokenIsAllowedOnManagerOnlyRoute() throws Exception {
        // manager@example.com is seeded by V15__seed_auth_data.sql
        RequestPostProcessor auth = loginAndGetToken("manager@example.com", "Password123");

        mockMvc.perform(get("/api/test/manager-only").with(auth))
                .andExpect(status().isOk());
    }
}
