package com.weeklyreportgenerator.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

import com.weeklyreportgenerator.backend.repository.ReportVersionRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class ReportSubmissionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final LocalDate BASE_MONDAY = LocalDate.of(2028, 1, 3);
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

    private Long createCompleteReport(String token, LocalDate weekStart) throws Exception {
        String body = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s","summary":"Weekly work",
                 "tasks":[{"taskName":"T1","status":"COMPLETED","priority":"HIGH","plannedPercentage":100,
                           "actualPercentage":100,"hoursPlanned":8,"hoursSpent":8}]}
                """.formatted(weekStart, weekStart.plusDays(6));

        String response = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return Long.valueOf(extractJsonStringValue(response, "\"id\":"));
    }

    private Long createDraftReport(String token, LocalDate weekStart, String extraJson) throws Exception {
        String body = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s"%s}
                """.formatted(weekStart, weekStart.plusDays(6), extraJson);

        String response = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return Long.valueOf(extractJsonStringValue(response, "\"id\":"));
    }

    @Test
    void submittingDraftSetsStatusSubmittedAndSubmittedAt() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        Long reportId = createCompleteReport(token, nextMonday());

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.submittedAt", notNullValue()))
                .andExpect(jsonPath("$.canEdit", is(false)));
    }

    @Test
    void submittingCreatesExactlyOneVersionNumberedOne() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        Long reportId = createCompleteReport(token, nextMonday());

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertThat(reportVersionRepository.findByReportId(reportId)).hasSize(1);
        assertThat(reportVersionRepository.findByReportId(reportId).get(0).getVersionNumber()).isEqualTo(1);
    }

    @Test
    void submittingAlreadySubmittedReportReturns409() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        Long reportId = createCompleteReport(token, nextMonday());

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("CONFLICT")));
    }

    @Test
    void submittingApprovedReportReturns409() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        Long reportId = createCompleteReport(token, nextMonday());

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // No APPROVE endpoint exists yet (added in the review step) -- force the state directly to
        // prove the workflow guard rejects submitting an already-APPROVED report.
        jdbcTemplate.update("UPDATE weekly_reports SET status = 'APPROVED' WHERE id = ?", reportId);

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void submittingReportWithZeroTasksReturns400() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        Long reportId = createDraftReport(token, nextMonday(), ",\"summary\":\"Has summary, no tasks\"");

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors[0]", notNullValue()));
    }

    @Test
    void submittingWithBlankSummaryReturns400() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        String extra = ",\"tasks\":[{\"taskName\":\"T1\",\"status\":\"COMPLETED\",\"priority\":\"HIGH\","
                + "\"plannedPercentage\":100,\"actualPercentage\":100,\"hoursPlanned\":8,\"hoursSpent\":8}]";
        Long reportId = createDraftReport(token, nextMonday(), extra);

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]", is("summary is required")));
    }

    @Test
    void badRequestListsAllValidationFailuresNotJustTheFirst() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        Long reportId = createDraftReport(token, nextMonday(), "");

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0]", is("At least one task is required before submitting")))
                .andExpect(jsonPath("$.errors[1]", is("summary is required")));
    }

    @Test
    void memberASubmittingMemberBsReportReturns404() throws Exception {
        String tokenA = loginAndGetToken("member@example.com", "Password123");
        String tokenB = loginAndGetToken("member1@weeklyreport.com", "Password123!");
        Long reportId = createCompleteReport(tokenB, nextMonday());

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void putOnSubmittedReportReturns409() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        LocalDate monday = nextMonday();
        Long reportId = createCompleteReport(token, monday);

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String body = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s","summary":"edited"}
                """.formatted(monday, monday.plusDays(6));

        mockMvc.perform(put("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteOnSubmittedReportReturns409() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        Long reportId = createCompleteReport(token, nextMonday());

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/reports/" + reportId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void snapshotRoundTripMatchesSubmittedContent() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        LocalDate monday = nextMonday();
        Long reportId = createCompleteReport(token, monday);

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/" + reportId + "/versions/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reportId.intValue())))
                .andExpect(jsonPath("$.weekStartDate", is(monday.toString())))
                .andExpect(jsonPath("$.weekEndDate", is(monday.plusDays(6).toString())))
                .andExpect(jsonPath("$.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.summary", is("Weekly work")))
                .andExpect(jsonPath("$.tasks", hasSize(1)))
                .andExpect(jsonPath("$.tasks[0].taskName", is("T1")));
    }

    @Test
    void unauthenticatedSubmitReturns401() throws Exception {
        mockMvc.perform(post("/api/reports/1/submit"))
                .andExpect(status().isUnauthorized());
    }
}
