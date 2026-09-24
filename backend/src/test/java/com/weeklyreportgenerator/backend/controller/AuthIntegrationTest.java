package com.weeklyreportgenerator.backend.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// There is no public registration any more -- accounts exist only through the admin invitation
// flow (see InvitationIntegrationTest) or the bootstrap admin. Fixture accounts here are created
// with a direct JDBC insert as a stand-in for "an account exists", since this file's job is to
// test login/me, not account creation.
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

    private String loginAndGetToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int start = response.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
        int end = response.indexOf('"', start);
        return response.substring(start, end);
    }

    @Test
    void loginWithCorrectCredentialsReturnsToken() throws Exception {
        String email = uniqueEmail("login-ok");
        createActiveTeamMember("Login User", email, "Password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "Password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.email", is(email)))
                .andExpect(jsonPath("$.user.role", is("TEAM_MEMBER")));
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
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void meWithValidTokenReturnsCorrectUser() throws Exception {
        String email = uniqueEmail("me-ok");
        createActiveTeamMember("Me User", email, "Password123");
        String token = loginAndGetToken(email, "Password123");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.role", is("TEAM_MEMBER")));
    }

    @Test
    void teamMemberTokenIsForbiddenOnManagerOnlyRoute() throws Exception {
        String email = uniqueEmail("rbac-member");
        createActiveTeamMember("RBAC Member", email, "Password123");
        String token = loginAndGetToken(email, "Password123");

        mockMvc.perform(get("/api/test/manager-only").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    void managerTokenIsAllowedOnManagerOnlyRoute() throws Exception {
        // manager@example.com is seeded by V15__seed_auth_data.sql
        String token = loginAndGetToken("manager@example.com", "Password123");

        mockMvc.perform(get("/api/test/manager-only").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
