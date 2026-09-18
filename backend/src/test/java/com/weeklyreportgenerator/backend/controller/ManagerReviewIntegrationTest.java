package com.weeklyreportgenerator.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.weeklyreportgenerator.backend.repository.ReportReviewRepository;
import com.weeklyreportgenerator.backend.repository.ReportVersionRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class ManagerReviewIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportReviewRepository reportReviewRepository;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    private static final LocalDate BASE_MONDAY = LocalDate.of(2029, 1, 1);
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

    private Long createAndSubmitReport(String memberToken, LocalDate weekStart, String summary) throws Exception {
        String body = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s","summary":"%s",
                 "tasks":[{"taskName":"T1","status":"COMPLETED","priority":"HIGH","plannedPercentage":100,
                           "actualPercentage":100,"hoursPlanned":8,"hoursSpent":8}]}
                """.formatted(weekStart, weekStart.plusDays(6), summary);

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

    // ---------- Role enforcement ----------

    @Test
    void teamMemberGetsForbiddenOnManagerReportsList() throws Exception {
        String token = memberToken();
        mockMvc.perform(get("/api/manager/reports").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void teamMemberGetsForbiddenOnManagerReportDetail() throws Exception {
        String memberToken = memberToken();
        Long reportId = createAndSubmitReport(memberToken, nextMonday(), "Forbidden detail test");

        mockMvc.perform(get("/api/manager/reports/" + reportId).header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void teamMemberGetsForbiddenOnTeamMembersList() throws Exception {
        String token = memberToken();
        mockMvc.perform(get("/api/manager/team-members").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void teamMemberGetsForbiddenOnApprove() throws Exception {
        String memberToken = memberToken();
        Long reportId = createAndSubmitReport(memberToken, nextMonday(), "Forbidden approve test");

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/approve")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teamMemberGetsForbiddenOnRequestChanges() throws Exception {
        String memberToken = memberToken();
        Long reportId = createAndSubmitReport(memberToken, nextMonday(), "Forbidden request-changes test");

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/request-changes")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Please add more detail here.\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedGetsUnauthorizedOnManagerReports() throws Exception {
        mockMvc.perform(get("/api/manager/reports")).andExpect(status().isUnauthorized());
    }

    @Test
    void managerCanReadReportOwnedByAnyMember() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        Long reportId = createAndSubmitReport(memberToken, nextMonday(), "Manager can read this");

        mockMvc.perform(get("/api/manager/reports/" + reportId).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.id", is(reportId.intValue())))
                .andExpect(jsonPath("$.ownerEmail", is("member@example.com")));
    }

    // ---------- Workflow ----------

    @Test
    void approveOnSubmittedTransitionsToApprovedAndWritesReviewWithCorrectVersion() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        Long reportId = createAndSubmitReport(memberToken, nextMonday(), "Approve workflow test");

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Good work\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.status", is("APPROVED")));

        assertThat(reportReviewRepository.findByReportId(reportId)).hasSize(1);
        assertThat(reportReviewRepository.findByReportId(reportId).get(0).getVersionNumber()).isEqualTo(1);
        assertThat(reportReviewRepository.findByReportId(reportId).get(0).getAction().name()).isEqualTo("APPROVED");
    }

    @Test
    void requestChangesOnSubmittedTransitionsToNeedsCorrectionAndPersistsComment() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        Long reportId = createAndSubmitReport(memberToken, nextMonday(), "Request changes workflow test");

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/request-changes")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Please add more detail here.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.status", is("NEEDS_CORRECTION")))
                .andExpect(jsonPath("$.reviews[0].comment", is("Please add more detail here.")));
    }

    @Test
    void requestChangesWithBlankCommentReturns400() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        Long reportId = createAndSubmitReport(memberToken, nextMonday(), "Blank comment test");

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/request-changes")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveOnDraftReturns409() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        LocalDate monday = nextMonday();
        String body = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s","summary":"still a draft"}
                """.formatted(monday, monday.plusDays(6));

        String response = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long reportId = Long.valueOf(extractJsonStringValue(response, "\"id\":"));

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void approveOnAlreadyApprovedReportReturns409() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        Long reportId = createAndSubmitReport(memberToken, nextMonday(), "Already approved test");

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void requestChangesOnNeedsCorrectionReportReturns409() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        Long reportId = createAndSubmitReport(memberToken, nextMonday(), "Needs correction repeat test");

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/request-changes")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Please add more detail here.\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/request-changes")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Please add even more detail here.\"}"))
                .andExpect(status().isConflict());
    }

    // ---------- Content protection ----------

    @Test
    void reviewRequestWithExtraContentFieldsLeavesReportContentUnchanged() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        Long reportId = createAndSubmitReport(memberToken, nextMonday(), "Content protection test");

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"comment":"Approved","summary":"HACKED","status":"DRAFT","tasks":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.status", is("APPROVED")))
                .andExpect(jsonPath("$.report.summary", is("Content protection test")))
                .andExpect(jsonPath("$.report.tasks", hasSize(1)));
    }

    // ---------- Full cycle ----------

    @Test
    void fullCycleFromDraftThroughCorrectionToApproval() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        LocalDate monday = nextMonday();

        Long reportId = createAndSubmitReport(memberToken, monday, "Original v1 content");

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/request-changes")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Please add more detail about testing.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.status", is("NEEDS_CORRECTION")));

        String editBody = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s","summary":"Edited v2 content",
                 "tasks":[{"taskName":"T1","status":"COMPLETED","priority":"HIGH","plannedPercentage":100,
                           "actualPercentage":100,"hoursPlanned":8,"hoursSpent":8}]}
                """.formatted(monday, monday.plusDays(6));

        mockMvc.perform(put("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentVersion", is(2)));

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Looks good now\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.status", is("APPROVED")));

        assertThat(reportVersionRepository.findByReportId(reportId)).hasSize(2);
        assertThat(reportReviewRepository.findByReportId(reportId)).hasSize(2);

        reportReviewRepository.findByReportId(reportId).forEach(review ->
                assertThat(review.getVersionNumber()).isNotNull());

        mockMvc.perform(get("/api/reports/" + reportId + "/versions/1").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary", is("Original v1 content")));

        mockMvc.perform(get("/api/reports/" + reportId + "/versions/2").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary", is("Edited v2 content")));
    }
}
