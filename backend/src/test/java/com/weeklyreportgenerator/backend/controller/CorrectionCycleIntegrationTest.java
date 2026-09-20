package com.weeklyreportgenerator.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
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

import com.weeklyreportgenerator.backend.entity.ReportReview;
import com.weeklyreportgenerator.backend.repository.ReportReviewRepository;
import com.weeklyreportgenerator.backend.repository.ReportVersionRepository;

// Verification-and-gap-fill pass for the correction cycle. Every scenario here runs through the
// existing ReportWorkflowService / PUT+submit endpoints -- no new service or state machine.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class CorrectionCycleIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    @Autowired
    private ReportReviewRepository reportReviewRepository;

    private static final LocalDate BASE_MONDAY = LocalDate.of(2030, 1, 7);
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

    private String otherMemberToken() throws Exception {
        return loginAndGetToken("member1@weeklyreport.com", "Password123!");
    }

    private String managerToken() throws Exception {
        return loginAndGetToken("manager@example.com", "Password123");
    }

    private String reportBody(LocalDate weekStart, String summary) {
        return """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s","summary":"%s",
                 "tasks":[{"taskName":"T1","status":"COMPLETED","priority":"HIGH","plannedPercentage":100,
                           "actualPercentage":100,"hoursPlanned":8,"hoursSpent":8}]}
                """.formatted(weekStart, weekStart.plusDays(6), summary);
    }

    private Long createReport(String token, LocalDate weekStart, String summary) throws Exception {
        String response = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBody(weekStart, summary)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.valueOf(extractJsonStringValue(response, "\"id\":"));
    }

    private void submit(String token, Long reportId) throws Exception {
        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void requestChanges(String managerToken, Long reportId, String comment) throws Exception {
        mockMvc.perform(post("/api/manager/reports/" + reportId + "/request-changes")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"%s\"}".formatted(comment)))
                .andExpect(status().isOk());
    }

    private Long createSubmitAndSendBack(String memberToken, String managerToken, LocalDate monday, String summary)
            throws Exception {
        Long reportId = createReport(memberToken, monday, summary);
        submit(memberToken, reportId);
        requestChanges(managerToken, reportId, "Please add more detail about testing.");
        return reportId;
    }

    @Test
    void editingNeedsCorrectionReportLeavesStatusAndVersionAlone() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        LocalDate monday = nextMonday();
        Long reportId = createSubmitAndSendBack(memberToken, managerToken, monday, "Original content");

        String editBody = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s","summary":"Edited but not resubmitted",
                 "tasks":[{"taskName":"T1","status":"COMPLETED","priority":"HIGH","plannedPercentage":100,
                           "actualPercentage":100,"hoursPlanned":8,"hoursSpent":8}]}
                """.formatted(monday, monday.plusDays(6));

        mockMvc.perform(put("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("NEEDS_CORRECTION")))
                .andExpect(jsonPath("$.currentVersion", is(1)));
    }

    @Test
    void resubmitProducesVersionTwoAndLeavesVersionOneSnapshotUntouched() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        LocalDate monday = nextMonday();
        Long reportId = createSubmitAndSendBack(memberToken, managerToken, monday, "Original v1 content");

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

        assertThat(reportVersionRepository.findByReportId(reportId)).hasSize(2);

        mockMvc.perform(get("/api/reports/" + reportId + "/versions/1").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary", is("Original v1 content")));

        mockMvc.perform(get("/api/reports/" + reportId + "/versions/2").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary", is("Edited v2 content")));
    }

    @Test
    void needsCorrectionQueueReturnsOnlyCallersReportsNeverAnotherMembers() throws Exception {
        String memberToken = memberToken();
        String otherMemberToken = otherMemberToken();
        String managerToken = managerToken();

        Long ownReportId = createSubmitAndSendBack(memberToken, managerToken, nextMonday(), "My own report");
        Long otherReportId = createSubmitAndSendBack(otherMemberToken, managerToken, nextMonday(), "Someone else's report");

        String response = mockMvc.perform(get("/api/reports/needs-correction")
                        .header("Authorization", "Bearer " + memberToken))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("\"id\":" + ownReportId);
        assertThat(response).doesNotContain("\"id\":" + otherReportId);
    }

    @Test
    void changingProjectOrWeekOnPreviouslySubmittedReportReturns409() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        LocalDate monday = nextMonday();
        Long reportId = createSubmitAndSendBack(memberToken, managerToken, monday, "Locked identity test");

        String changedProject = """
                {"projectId":2,"weekStartDate":"%s","weekEndDate":"%s","summary":"trying to switch project"}
                """.formatted(monday, monday.plusDays(6));
        mockMvc.perform(put("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changedProject))
                .andExpect(status().isConflict());

        LocalDate otherMonday = monday.plusWeeks(10);
        String changedWeek = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s","summary":"trying to switch week"}
                """.formatted(otherMonday, otherMonday.plusDays(6));
        mockMvc.perform(put("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changedWeek))
                .andExpect(status().isConflict());
    }

    @Test
    void acknowledgeSetsFlagAndIsIdempotent() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        Long reportId = createSubmitAndSendBack(memberToken, managerToken, nextMonday(), "Acknowledge test");

        String detail = mockMvc.perform(get("/api/reports/" + reportId).header("Authorization", "Bearer " + memberToken))
                .andExpect(jsonPath("$.hasUnreadReview", is(true)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        // The top-level report also has an "id" field before "reviews" -- search from there on.
        String reviewsSection = detail.substring(detail.indexOf("\"reviews\":"));
        Long reviewId = Long.valueOf(extractJsonStringValue(reviewsSection, "\"id\":"));

        mockMvc.perform(post("/api/reports/" + reportId + "/reviews/" + reviewId + "/acknowledge")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/reports/" + reportId).header("Authorization", "Bearer " + memberToken))
                .andExpect(jsonPath("$.hasUnreadReview", is(false)));

        mockMvc.perform(post("/api/reports/" + reportId + "/reviews/" + reviewId + "/acknowledge")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void acknowledgingAnotherMembersReviewReturns404() throws Exception {
        String memberToken = memberToken();
        String otherMemberToken = otherMemberToken();
        String managerToken = managerToken();
        Long reportId = createSubmitAndSendBack(memberToken, managerToken, nextMonday(), "Cross-member acknowledge test");

        List<ReportReview> reviews = reportReviewRepository.findByReportId(reportId);
        Long reviewId = reviews.get(0).getId();

        mockMvc.perform(post("/api/reports/" + reportId + "/reviews/" + reviewId + "/acknowledge")
                        .header("Authorization", "Bearer " + otherMemberToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void resubmittingWithZeroContentChangesReturns409() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        Long reportId = createSubmitAndSendBack(memberToken, managerToken, nextMonday(), "No-op resubmit test");

        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is(
                        "No changes were made since the manager's feedback -- edit the report before resubmitting")));
    }

    @Test
    void resubmittingAfterARealEditSucceeds() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        LocalDate monday = nextMonday();
        Long reportId = createSubmitAndSendBack(memberToken, managerToken, monday, "Real edit test");

        String editBody = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s","summary":"Genuinely edited content",
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
                .andExpect(jsonPath("$.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.currentVersion", is(2)));
    }

    @Test
    void secondCorrectionRoundProducesThreeVersionsAndThreeReviews() throws Exception {
        String memberToken = memberToken();
        String managerToken = managerToken();
        LocalDate monday = nextMonday();

        Long reportId = createSubmitAndSendBack(memberToken, managerToken, monday, "v1 content");

        String v2Body = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s","summary":"v2 content",
                 "tasks":[{"taskName":"T1","status":"COMPLETED","priority":"HIGH","plannedPercentage":100,
                           "actualPercentage":100,"hoursPlanned":8,"hoursSpent":8}]}
                """.formatted(monday, monday.plusDays(6));
        mockMvc.perform(put("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(v2Body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentVersion", is(2)));

        requestChanges(managerToken, reportId, "Second round: please clarify further.");

        String v3Body = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s","summary":"v3 content",
                 "tasks":[{"taskName":"T1","status":"COMPLETED","priority":"HIGH","plannedPercentage":100,
                           "actualPercentage":100,"hoursPlanned":8,"hoursSpent":8}]}
                """.formatted(monday, monday.plusDays(6));
        mockMvc.perform(put("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(v3Body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentVersion", is(3)));

        assertThat(reportVersionRepository.findByReportId(reportId)).hasSize(3);
        assertThat(reportReviewRepository.findByReportId(reportId)).hasSize(2);

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Approved on third try\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.status", is("APPROVED")));

        assertThat(reportVersionRepository.findByReportId(reportId)).hasSize(3);
        assertThat(reportReviewRepository.findByReportId(reportId)).hasSize(3);

        mockMvc.perform(get("/api/reports/" + reportId + "/versions/1").header("Authorization", "Bearer " + memberToken))
                .andExpect(jsonPath("$.summary", is("v1 content")));
        mockMvc.perform(get("/api/reports/" + reportId + "/versions/2").header("Authorization", "Bearer " + memberToken))
                .andExpect(jsonPath("$.summary", is("v2 content")));
        mockMvc.perform(get("/api/reports/" + reportId + "/versions/3").header("Authorization", "Bearer " + memberToken))
                .andExpect(jsonPath("$.summary", is("v3 content")));
    }
}
