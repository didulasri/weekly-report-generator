package com.weeklyreportgenerator.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import jakarta.servlet.http.Cookie;

// Cookie-based auth + refresh rotation (checkpoint 5). Deliberately drives raw MockMvc cookie
// plumbing instead of the RequestPostProcessor helper the other integration test classes use --
// that helper hides exactly the cookie lifecycle this file exists to exercise (which cookie is
// valid when, and what happens when an old one is replayed).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class RefreshTokenIntegrationTest {

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

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private Cookie xsrfFor(Cookie accessToken, Cookie refreshToken) throws Exception {
        return mockMvc.perform(get("/api/auth/csrf").cookie(accessToken, refreshToken))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
    }

    @Test
    void loginResponseBodyNeverContainsEitherToken() throws Exception {
        MvcResult result = login("member@example.com", "Password123");
        String body = result.getResponse().getContentAsString();
        Cookie accessTokenCookie = result.getResponse().getCookie("access_token");
        Cookie refreshTokenCookie = result.getResponse().getCookie("refresh_token");

        assertThat(body).doesNotContain(accessTokenCookie.getValue());
        assertThat(body).doesNotContain(refreshTokenCookie.getValue());
    }

    @Test
    void meWorksWithAccessTokenCookieAndFailsWithNeitherCookie() throws Exception {
        MvcResult result = login("member@example.com", "Password123");
        Cookie accessTokenCookie = result.getResponse().getCookie("access_token");

        mockMvc.perform(get("/api/auth/me").cookie(accessTokenCookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anAuthorizationHeaderBearerTokenIsIgnored() throws Exception {
        // There is no header-based auth path any more -- JwtAuthenticationFilter only ever reads
        // the access_token cookie. A well-formed JWT in the Authorization header must not work.
        MvcResult result = login("member@example.com", "Password123");
        Cookie accessTokenCookie = result.getResponse().getCookie("access_token");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessTokenCookie.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutTheCookieReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCsrfHeaderIsRejected() throws Exception {
        MvcResult result = login("member@example.com", "Password123");
        Cookie refreshTokenCookie = result.getResponse().getCookie("refresh_token");

        // No X-XSRF-TOKEN header -- CSRF protection applies to /api/auth/refresh (it is not in the
        // ignored-matchers list, unlike /login).
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshTokenCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshIssuesANewAccessTokenAndRotatesTheRefreshCookie() throws Exception {
        MvcResult loginResult = login("member@example.com", "Password123");
        Cookie oldAccessCookie = loginResult.getResponse().getCookie("access_token");
        Cookie oldRefreshCookie = loginResult.getResponse().getCookie("refresh_token");
        Cookie xsrf = xsrfFor(oldAccessCookie, oldRefreshCookie);

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(oldRefreshCookie, xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isOk())
                .andReturn();

        Cookie newAccessCookie = refreshResult.getResponse().getCookie("access_token");
        Cookie newRefreshCookie = refreshResult.getResponse().getCookie("refresh_token");
        assertThat(newAccessCookie).isNotNull();
        assertThat(newRefreshCookie).isNotNull();
        assertThat(newRefreshCookie.getValue()).isNotEqualTo(oldRefreshCookie.getValue());

        // The new access token actually works.
        mockMvc.perform(get("/api/auth/me").cookie(newAccessCookie))
                .andExpect(status().isOk());
    }

    @Test
    void replayingAnAlreadyRotatedRefreshTokenRevokesTheWholeFamily() throws Exception {
        MvcResult loginResult = login("member@example.com", "Password123");
        Cookie accessCookie = loginResult.getResponse().getCookie("access_token");
        Cookie firstRefreshCookie = loginResult.getResponse().getCookie("refresh_token");
        Cookie xsrf = xsrfFor(accessCookie, firstRefreshCookie);

        // Rotate once -- firstRefreshCookie is now revoked (replaced by a second token).
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(firstRefreshCookie, xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isOk())
                .andReturn();
        Cookie secondRefreshCookie = refreshResult.getResponse().getCookie("refresh_token");

        // Replay the already-rotated first token -- this is the reuse-detection path, which must
        // revoke the entire family, including the second (currently valid) token.
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(firstRefreshCookie, xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isUnauthorized());

        // The second token, which was valid a moment ago, must now be rejected too -- proof the
        // whole family was revoked, not just the replayed token.
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(secondRefreshCookie, xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithAnUnknownTokenReturns401AndClearsBothCookies() throws Exception {
        // CookieCsrfTokenRepository is a stateless double-submit check (cookie value must match the
        // header) -- it isn't bound to any particular user or refresh token, so a garbage refresh
        // token still needs a genuine matching XSRF pair to even reach the refresh-token lookup.
        Cookie xsrf = xsrfFor(new Cookie("access_token", "irrelevant"), new Cookie("refresh_token", "not-a-real-token"));

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", "not-a-real-token"), xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Cookie clearedAccess = result.getResponse().getCookie("access_token");
        Cookie clearedRefresh = result.getResponse().getCookie("refresh_token");
        assertThat(clearedAccess.getMaxAge()).isEqualTo(0);
        assertThat(clearedRefresh.getMaxAge()).isEqualTo(0);
    }

    @Test
    void logoutRevokesTheRefreshTokenAndClearsBothCookies() throws Exception {
        MvcResult loginResult = login("member@example.com", "Password123");
        Cookie accessCookie = loginResult.getResponse().getCookie("access_token");
        Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");
        Cookie xsrf = xsrfFor(accessCookie, refreshCookie);

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .cookie(refreshCookie, xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie clearedAccess = logoutResult.getResponse().getCookie("access_token");
        Cookie clearedRefresh = logoutResult.getResponse().getCookie("refresh_token");
        assertThat(clearedAccess.getMaxAge()).isEqualTo(0);
        assertThat(clearedRefresh.getMaxAge()).isEqualTo(0);

        // The now-revoked refresh token can no longer be used to get a new session.
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie, xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatingAnAccountRevokesItsRefreshToken() throws Exception {
        String email = uniqueEmail("deactivate-revoke");
        createActiveTeamMember("Deactivate Revoke", email);
        MvcResult victimLogin = login(email, "Password123");
        Cookie victimRefresh = victimLogin.getResponse().getCookie("refresh_token");
        Cookie victimXsrf = xsrfFor(victimLogin.getResponse().getCookie("access_token"), victimRefresh);

        // Sanity check: the token works before deactivation.
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(victimRefresh, victimXsrf)
                        .header("X-XSRF-TOKEN", victimXsrf.getValue()))
                .andExpect(status().isOk());

        deactivate(email);

        // Re-derive fresh cookies for the next call, since the previous /refresh already rotated
        // victimRefresh -- the point being tested is that deactivation revokes whatever refresh
        // token the user is currently holding, so issue one more rotation first, then deactivate,
        // then confirm the latest token is rejected.
        MvcResult secondLogin = login(email, "Password123");
        Cookie latestRefresh = secondLogin.getResponse().getCookie("refresh_token");
        Cookie latestXsrf = xsrfFor(secondLogin.getResponse().getCookie("access_token"), latestRefresh);
        deactivate(email);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(latestRefresh, latestXsrf)
                        .header("X-XSRF-TOKEN", latestXsrf.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changingAnAccountsRoleRevokesItsRefreshToken() throws Exception {
        String email = uniqueEmail("role-change-revoke");
        createActiveTeamMember("Role Change Revoke", email);
        MvcResult victimLogin = login(email, "Password123");
        Cookie victimRefresh = victimLogin.getResponse().getCookie("refresh_token");
        Cookie victimXsrf = xsrfFor(victimLogin.getResponse().getCookie("access_token"), victimRefresh);

        changeRole(email, "MANAGER");

        // The old refresh token, issued under the old role, must no longer work -- otherwise a
        // demoted (or promoted) user could keep refreshing a session that carries stale claims.
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(victimRefresh, victimXsrf)
                        .header("X-XSRF-TOKEN", victimXsrf.getValue()))
                .andExpect(status().isUnauthorized());

        // But the account itself still works -- a fresh login picks up the new role.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password123\"}"))
                .andExpect(status().isOk());
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + System.nanoTime() + "@example.com";
    }

    private void createActiveTeamMember(String name, String email) {
        Long roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = 'TEAM_MEMBER'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO users (name, email, password, role_id, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, true, now(), now())
                """, name, email, passwordEncoder.encode("Password123"), roleId);
    }

    private void deactivate(String email) throws Exception {
        Long id = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        // Deactivation is idempotent-ish for this test's purposes: a second call on an
        // already-deactivated account would 409/404 depending on state, so only deactivate once by
        // checking active first.
        Boolean active = jdbcTemplate.queryForObject("SELECT active FROM users WHERE id = ?", Boolean.class, id);
        if (Boolean.FALSE.equals(active)) {
            return;
        }
        MvcResult adminLogin = login("admin@example.com", "Password123");
        Cookie adminAccess = adminLogin.getResponse().getCookie("access_token");
        Cookie adminRefresh = adminLogin.getResponse().getCookie("refresh_token");
        Cookie adminXsrf = xsrfFor(adminAccess, adminRefresh);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/admin/users/" + id)
                        .cookie(adminAccess, adminRefresh, adminXsrf)
                        .header("X-XSRF-TOKEN", adminXsrf.getValue()))
                .andExpect(status().isNoContent());
    }

    private void changeRole(String email, String newRole) throws Exception {
        Long id = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        MvcResult adminLogin = login("admin@example.com", "Password123");
        Cookie adminAccess = adminLogin.getResponse().getCookie("access_token");
        Cookie adminRefresh = adminLogin.getResponse().getCookie("refresh_token");
        Cookie adminXsrf = xsrfFor(adminAccess, adminRefresh);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/admin/users/" + id + "/role")
                        .cookie(adminAccess, adminRefresh, adminXsrf)
                        .header("X-XSRF-TOKEN", adminXsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"" + newRole + "\"}"))
                .andExpect(status().isOk());
    }
}
