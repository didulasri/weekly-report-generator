package com.weeklyreportgenerator.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

// Admin user-management module. Every guard lives in AdminUserService (self-role-change,
// self-deactivation, last-active-admin), enforced with @PreAuthorize on the service method --
// these tests exercise them through the real HTTP endpoints, not by calling the service directly.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class AdminUserIntegrationTest {

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

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private String loginAndGetToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractJsonValue(response, "\"accessToken\":\"", true);
    }

    private String extractJsonValue(String json, String key, boolean quoted) {
        int start = json.indexOf(key) + key.length();
        if (quoted) {
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return json.substring(start, end);
    }

    private String adminToken() throws Exception {
        return loginAndGetToken("admin@example.com", "Password123");
    }

    private String managerToken() throws Exception {
        return loginAndGetToken("manager@example.com", "Password123");
    }

    private String memberToken() throws Exception {
        return loginAndGetToken("member@example.com", "Password123");
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + COUNTER.incrementAndGet() + "-" + System.nanoTime() + "@example.com";
    }

    // Account creation moved to the invitation flow -- POST /api/admin/users no longer exists.
    // This helper stands in for "an active account with this role exists" via a direct insert, the
    // same bridge used in AuthIntegrationTest and DashboardFixtureIntegrationTest. The adminToken
    // parameter is kept (unused) so every existing call site in this file needed no other change.
    private Long createUser(String adminToken, String name, String email, String role) {
        Long roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = ?", Long.class, role);
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (name, email, password, role_id, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, true, now(), now())
                RETURNING id
                """, Long.class, name, email, passwordEncoder.encode("Password123"), roleId);
    }

    // ---- Role enforcement ----

    private static final List<String> GET_ENDPOINTS = List.of("/api/admin/users", "/api/admin/users/1");

    @Test
    void teamMemberGetsForbiddenOnEveryAdminEndpoint() throws Exception {
        String token = memberToken();
        for (String url : GET_ENDPOINTS) {
            mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(put("/api/admin/users/1").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"x@example.com\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/users/1/role").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MANAGER\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/users/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/users/1/activate").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerGetsForbiddenOnEveryAdminEndpoint() throws Exception {
        String token = managerToken();
        for (String url : GET_ENDPOINTS) {
            mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(put("/api/admin/users/1").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"x@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedGetsUnauthorizedOnEveryAdminEndpoint() throws Exception {
        for (String url : GET_ENDPOINTS) {
            mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(put("/api/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"x@example.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminSucceedsOnListAndDetail() throws Exception {
        String token = adminToken();
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/users/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ---- Privilege escalation guards ----

    @Test
    void adminChangingOwnRoleReturnsConflict() throws Exception {
        String token = adminToken();
        Long selfId = Long.valueOf(extractJsonValue(
                mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                        .andReturn().getResponse().getContentAsString(),
                "\"id\":", false));

        mockMvc.perform(patch("/api/admin/users/" + selfId + "/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MANAGER\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void adminDeactivatingSelfReturnsConflict() throws Exception {
        String token = adminToken();
        Long selfId = Long.valueOf(extractJsonValue(
                mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                        .andReturn().getResponse().getContentAsString(),
                "\"id\":", false));

        mockMvc.perform(delete("/api/admin/users/" + selfId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void demotingTheLastActiveAdminReturnsConflict() throws Exception {
        String adminToken = adminToken();
        // Solo admin scenario is only reachable by the sole remaining admin acting on themselves,
        // since @PreAuthorize("hasRole('ADMIN')") means only an active admin can call this endpoint
        // at all -- so this necessarily exercises the same call path as the self-guard test, and
        // both guards independently return 409 for it.
        Long selfId = Long.valueOf(extractJsonValue(
                mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + adminToken))
                        .andReturn().getResponse().getContentAsString(),
                "\"id\":", false));

        mockMvc.perform(patch("/api/admin/users/" + selfId + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TEAM_MEMBER\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deactivatingTheLastActiveAdminReturnsConflict() throws Exception {
        String adminToken = adminToken();
        Long selfId = Long.valueOf(extractJsonValue(
                mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + adminToken))
                        .andReturn().getResponse().getContentAsString(),
                "\"id\":", false));

        mockMvc.perform(delete("/api/admin/users/" + selfId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void withTwoActiveAdminsDemotingOneSucceeds() throws Exception {
        String adminToken = adminToken();
        Long secondAdminId = createUser(adminToken, "Second Admin", uniqueEmail("second-admin"), "ADMIN");

        mockMvc.perform(patch("/api/admin/users/" + secondAdminId + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MANAGER\"}"))
                .andExpect(status().isOk());
    }

    // ---- Behaviour ----
    // Creation-specific behaviour (duplicate email, invalid role, BCrypt hashing on create) moved
    // to InvitationIntegrationTest, since POST /api/admin/users no longer exists -- account
    // creation is invitation-only.

    @Test
    void noResponseBodyContainsAPasswordField() throws Exception {
        String adminToken = adminToken();
        String email = uniqueEmail("no-echo");
        Long id = createUser(adminToken, "No Echo", email, "TEAM_MEMBER");

        String listResponse = mockMvc.perform(get("/api/admin/users?search=" + email)
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getContentAsString();
        assertThat(listResponse.toLowerCase()).doesNotContain("\"password\"");

        String detailResponse = mockMvc.perform(get("/api/admin/users/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getContentAsString();
        assertThat(detailResponse.toLowerCase()).doesNotContain("\"password\"");
    }

    @Test
    void deleteSetsActiveFalseAndRowStillExists() throws Exception {
        String adminToken = adminToken();
        String email = uniqueEmail("soft-delete");
        Long id = createUser(adminToken, "Soft Delete", email, "TEAM_MEMBER");

        mockMvc.perform(delete("/api/admin/users/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id);
        assertThat(rowCount).isEqualTo(1);

        Boolean active = jdbcTemplate.queryForObject(
                "SELECT active FROM users WHERE id = ?", Boolean.class, id);
        assertThat(active).isFalse();
    }

    @Test
    void deactivatedUsersExistingReportsAreStillVisibleToManager() throws Exception {
        String adminToken = adminToken();
        String managerToken = managerToken();
        String email = uniqueEmail("report-owner");
        Long memberId = createUser(adminToken, "Report Owner", email, "TEAM_MEMBER");
        String memberToken = loginAndGetToken(email, "Password123");

        // Project 1 is already active with member assignment allowed (see V16 seed) -- reuse it
        // rather than standing up a fresh project just to attach one report to it.
        mockMvc.perform(post("/api/projects/1/members")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + memberId + "}"))
                .andExpect(status().isCreated());

        String reportResponse = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":1,"weekStartDate":"2031-03-03","weekEndDate":"2031-03-09",
                                 "summary":"Visible after deactivation",
                                 "tasks":[{"taskName":"T1","status":"COMPLETED","priority":"HIGH",
                                           "plannedPercentage":100,"actualPercentage":100,
                                           "hoursPlanned":8,"hoursSpent":8}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long reportId = Long.valueOf(extractJsonValue(reportResponse, "\"id\":", false));

        mockMvc.perform(delete("/api/admin/users/" + memberId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        String managerViewResponse = mockMvc.perform(get("/api/manager/reports/" + reportId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(managerViewResponse).contains("Visible after deactivation");

        String teamReportsResponse = mockMvc.perform(get("/api/manager/reports?userId=" + memberId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(extractJsonValue(teamReportsResponse, "\"totalElements\":", false)).isEqualTo("1");
    }

    @Test
    void deactivatedUsersValidTokenReturnsUnauthorizedOnNextRequest() throws Exception {
        String adminToken = adminToken();
        String email = uniqueEmail("token-kill");
        createUser(adminToken, "Token Kill", email, "TEAM_MEMBER");

        String victimToken = loginAndGetToken(email, "Password123");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + victimToken))
                .andExpect(status().isOk());

        Long id = Long.valueOf(extractJsonValue(
                mockMvc.perform(get("/api/admin/users?search=" + email)
                                .header("Authorization", "Bearer " + adminToken))
                        .andReturn().getResponse().getContentAsString(),
                "\"id\":", false));
        mockMvc.perform(delete("/api/admin/users/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + victimToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void activateRestoresLogin() throws Exception {
        String adminToken = adminToken();
        String email = uniqueEmail("reactivate");
        Long id = createUser(adminToken, "Reactivate Me", email, "TEAM_MEMBER");

        mockMvc.perform(delete("/api/admin/users/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Password123\"}".formatted(email)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/admin/users/" + id + "/activate").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Password123\"}".formatted(email)))
                .andExpect(status().isOk());
    }

    // adminPasswordResetLetsUserLoginWithNewPassword moved to the password-reset checkpoint --
    // PATCH /api/admin/users/{id}/reset-password no longer exists, replaced by
    // POST /api/admin/users/{id}/send-password-reset (an email-triggering flow, not a direct set).

    @Test
    void userListSearchAndRoleFiltersReturnTheRightSubsets() throws Exception {
        String adminToken = adminToken();
        String marker = "srch" + System.nanoTime();
        createUser(adminToken, "Filter Manager", marker + "-mgr@example.com", "MANAGER");
        createUser(adminToken, "Filter Member", marker + "-mem@example.com", "TEAM_MEMBER");

        String searchResponse = mockMvc.perform(get("/api/admin/users?search=" + marker)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(searchResponse).contains(marker + "-mgr@example.com");
        assertThat(searchResponse).contains(marker + "-mem@example.com");
        assertThat(extractJsonValue(searchResponse, "\"totalElements\":", false)).isEqualTo("2");

        String roleFilteredResponse = mockMvc.perform(
                        get("/api/admin/users?search=" + marker + "&role=MANAGER")
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(roleFilteredResponse).contains(marker + "-mgr@example.com");
        assertThat(roleFilteredResponse).doesNotContain(marker + "-mem@example.com");
    }
}
