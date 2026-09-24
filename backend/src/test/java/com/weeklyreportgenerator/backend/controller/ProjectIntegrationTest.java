package com.weeklyreportgenerator.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.weeklyreportgenerator.backend.entity.Project;
import com.weeklyreportgenerator.backend.entity.enums.ProjectStatus;
import com.weeklyreportgenerator.backend.repository.ProjectRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class ProjectIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    private String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private String projectBody(String name, String description) {
        return """
                {"name":"%s","description":"%s"}
                """.formatted(name, description);
    }

    // Logs in (cookies, not a Bearer token) and returns a RequestPostProcessor bundling the
    // access_token + refresh_token cookies plus the X-XSRF-TOKEN header -- apply with .with(...).
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

    private Long loginAndGetUserId(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String idStr = extractJsonStringValue(response, "\"id\":");
        return Long.valueOf(idStr);
    }

    private String extractJsonStringValue(String json, String key) {
        int start = json.indexOf(key) + key.length();
        boolean quoted = json.charAt(start) == '"';
        if (quoted) {
            start++;
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        }
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return json.substring(start, end);
    }

    private Long createProjectAsManager(RequestPostProcessor managerToken, String name) throws Exception {
        String response = mockMvc.perform(post("/api/projects")
                        .with(managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(name, "Created for tests")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return Long.valueOf(extractJsonStringValue(response, "\"id\":"));
    }

    @Test
    void teamMemberGets200OnListProjects() throws Exception {
        RequestPostProcessor token = loginAndGetToken("member@example.com", "Password123");

        mockMvc.perform(get("/api/projects").with(token))
                .andExpect(status().isOk());
    }

    @Test
    void teamMemberGets403OnCreateProject() throws Exception {
        RequestPostProcessor token = loginAndGetToken("member@example.com", "Password123");

        mockMvc.perform(post("/api/projects")
                        .with(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(uniqueName("forbidden-create"), "desc")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    void teamMemberGets403OnUpdateProject() throws Exception {
        RequestPostProcessor managerToken = loginAndGetToken("manager@example.com", "Password123");
        RequestPostProcessor memberToken = loginAndGetToken("member@example.com", "Password123");
        Long projectId = createProjectAsManager(managerToken, uniqueName("forbidden-update"));

        mockMvc.perform(put("/api/projects/" + projectId)
                        .with(memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(uniqueName("forbidden-update-renamed"), "desc")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teamMemberGets403OnDeleteProject() throws Exception {
        RequestPostProcessor managerToken = loginAndGetToken("manager@example.com", "Password123");
        RequestPostProcessor memberToken = loginAndGetToken("member@example.com", "Password123");
        Long projectId = createProjectAsManager(managerToken, uniqueName("forbidden-delete"));

        mockMvc.perform(delete("/api/projects/" + projectId)
                        .with(memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerGets201OnCreateProject() throws Exception {
        RequestPostProcessor managerToken = loginAndGetToken("manager@example.com", "Password123");

        mockMvc.perform(post("/api/projects")
                        .with(managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(uniqueName("manager-create"), "desc")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void duplicateProjectNameReturns409() throws Exception {
        RequestPostProcessor managerToken = loginAndGetToken("manager@example.com", "Password123");
        String name = uniqueName("duplicate");
        createProjectAsManager(managerToken, name);

        mockMvc.perform(post("/api/projects")
                        .with(managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(name.toUpperCase(), "another desc")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("CONFLICT")));
    }

    @Test
    void unknownProjectIdReturns404() throws Exception {
        RequestPostProcessor token = loginAndGetToken("member@example.com", "Password123");

        mockMvc.perform(get("/api/projects/999999999").with(token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }

    @Test
    void deleteReturns204AndRowStillExistsAsInactive() throws Exception {
        RequestPostProcessor managerToken = loginAndGetToken("manager@example.com", "Password123");
        Long projectId = createProjectAsManager(managerToken, uniqueName("soft-delete"));

        mockMvc.perform(delete("/api/projects/" + projectId)
                        .with(managerToken))
                .andExpect(status().isNoContent());

        Project project = projectRepository.findById(projectId).orElseThrow();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.INACTIVE);
    }

    @Test
    void assigningSameUserTwiceReturns409() throws Exception {
        RequestPostProcessor managerToken = loginAndGetToken("manager@example.com", "Password123");
        Long projectId = createProjectAsManager(managerToken, uniqueName("assign-twice"));
        Long memberUserId = loginAndGetUserId("member@example.com", "Password123");

        String assignBody = "{\"userId\":" + memberUserId + "}";

        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .with(managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .with(managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("CONFLICT")));
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }
}
