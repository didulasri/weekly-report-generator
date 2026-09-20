package com.weeklyreportgenerator.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class DashboardIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private static final LocalDate BASE_MONDAY = LocalDate.of(2031, 1, 6);
    private static final AtomicInteger WEEK_OFFSET = new AtomicInteger(0);

    private LocalDate nextMonday() {
        return BASE_MONDAY.plusWeeks(WEEK_OFFSET.getAndIncrement());
    }

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

        return extractJsonStringValue(response, "\"accessToken\":\"");
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

    private String memberToken() throws Exception {
        return loginAndGetToken("member@example.com", "Password123");
    }

    private String managerToken() throws Exception {
        return loginAndGetToken("manager@example.com", "Password123");
    }

    private Long createAndSubmitReport(String memberToken, LocalDate weekStart) throws Exception {
        String body = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s","summary":"Activity feed stability test",
                 "tasks":[{"taskName":"T1","status":"COMPLETED","priority":"HIGH","plannedPercentage":100,
                           "actualPercentage":100,"hoursPlanned":8,"hoursSpent":8}]}
                """.formatted(weekStart, weekStart.plusDays(6));

        String response = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long reportId = Long.valueOf(extractJsonStringValue(response, "\"id\":"));

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        return reportId;
    }

    // Fetches every page of the activity feed (using the size the test asks for) and returns the
    // concatenated content, so the test can inspect the whole feed rather than one page in isolation.
    private List<JsonNode> fetchAllPages(String token, int pageSize) throws Exception {
        List<JsonNode> all = new ArrayList<>();
        int page = 0;
        while (true) {
            String response = mockMvc.perform(get("/api/dashboard/activity-feed")
                            .param("page", String.valueOf(page))
                            .param("size", String.valueOf(pageSize))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            JsonNode root = objectMapper.readTree(response);
            root.get("content").forEach(all::add);

            boolean last = root.get("last").asBoolean();
            if (last) {
                break;
            }
            page++;
        }
        return all;
    }

    // Regression test for the unstable-sort bug: ORDER BY occurred_at DESC alone gives ties no
    // defined order, so paginating a set of same-timestamp rows could return a row twice or skip
    // it entirely between page 0 and page 1. event_type ASC, id DESC as tiebreakers fix that.
    @Test
    void activityFeedPaginationIsStableWhenRowsShareAnIdenticalTimestamp() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();

        Long reportA = createAndSubmitReport(memberToken, nextMonday());
        Long reportB = createAndSubmitReport(memberToken, nextMonday());

        mockMvc.perform(post("/api/manager/reports/" + reportA + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Stability test approval\"}"))
                .andExpect(status().isOk());

        // Force all three events (2 submissions + 1 review) onto the exact same instant -- this is
        // the scenario that exposed the bug: every tie must still resolve deterministically.
        Instant sharedInstant = Instant.parse("2031-06-01T12:00:00Z");
        jdbcTemplate.update(
                "UPDATE report_versions SET submitted_at = ? WHERE report_id IN (?, ?)",
                java.sql.Timestamp.from(sharedInstant), reportA, reportB);
        jdbcTemplate.update(
                "UPDATE report_reviews SET reviewed_at = ? WHERE report_id = ?",
                java.sql.Timestamp.from(sharedInstant), reportA);

        // Run the full pagination sweep several times -- an unstable ORDER BY would not necessarily
        // fail on every single run, so repetition is what catches the nondeterminism.
        for (int run = 0; run < 5; run++) {
            List<JsonNode> allEntries = fetchAllPages(managerToken, 2);

            List<JsonNode> matchingA = allEntries.stream()
                    .filter(e -> e.get("reportId").asLong() == reportA)
                    .toList();
            List<JsonNode> matchingB = allEntries.stream()
                    .filter(e -> e.get("reportId").asLong() == reportB)
                    .toList();

            // Report A produced exactly one SUBMISSION and one REVIEW event; report B exactly one
            // SUBMISSION. Each must appear exactly once across the whole paginated sweep.
            assertThat(matchingA).hasSize(2);
            assertThat(matchingA.stream().map(e -> e.get("type").asText()).sorted().toList())
                    .containsExactly("REVIEW", "SUBMISSION");
            assertThat(matchingB).hasSize(1);
            assertThat(matchingB.get(0).get("type").asText()).isEqualTo("SUBMISSION");
        }
    }
}
