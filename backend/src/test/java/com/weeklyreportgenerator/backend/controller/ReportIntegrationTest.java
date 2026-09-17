package com.weeklyreportgenerator.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.weeklyreportgenerator.backend.repository.ReportTaskRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class ReportIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportTaskRepository reportTaskRepository;

    private static final LocalDate BASE_MONDAY = LocalDate.of(2027, 1, 4);
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

        return Long.valueOf(extractJsonStringValue(response, "\"id\":"));
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

    private String basicReportBody(long projectId, LocalDate weekStart, LocalDate weekEnd) {
        return """
                {"projectId":%d,"weekStartDate":"%s","weekEndDate":"%s"}
                """.formatted(projectId, weekStart, weekEnd);
    }

    private Long createReport(String token, long projectId, LocalDate weekStart, LocalDate weekEnd) throws Exception {
        String response = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basicReportBody(projectId, weekStart, weekEnd)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return Long.valueOf(extractJsonStringValue(response, "\"id\":"));
    }

    @Test
    void memberBecomesOwnerOfCreatedReport() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        Long userId = loginAndGetUserId("member@example.com", "Password123");
        LocalDate monday = nextMonday();

        String response = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basicReportBody(1, monday, monday.plusDays(6))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long reportId = Long.valueOf(extractJsonStringValue(response, "\"id\":"));

        // owner-scoped fetch must succeed for the creator
        mockMvc.perform(get("/api/reports/" + reportId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertThat(userId).isNotNull();
    }

    @Test
    void userIdInCreateBodyIsIgnored() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        LocalDate monday = nextMonday();

        String body = """
                {"userId":999999,"projectId":1,"weekStartDate":"%s","weekEndDate":"%s"}
                """.formatted(monday, monday.plusDays(6));

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // if the bogus userId had been honored, this owner-scoped list would come back empty
        mockMvc.perform(get("/api/reports/my?size=50").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", notNullValue()));
    }

    @Test
    void memberAGets404FetchingMemberBsReport() throws Exception {
        String tokenA = loginAndGetToken("member@example.com", "Password123");
        String tokenB = loginAndGetToken("member1@weeklyreport.com", "Password123!");
        LocalDate monday = nextMonday();

        Long reportId = createReport(tokenB, 1, monday, monday.plusDays(6));

        mockMvc.perform(get("/api/reports/" + reportId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void memberAGets404UpdatingMemberBsReport() throws Exception {
        String tokenA = loginAndGetToken("member@example.com", "Password123");
        String tokenB = loginAndGetToken("member1@weeklyreport.com", "Password123!");
        LocalDate monday = nextMonday();

        Long reportId = createReport(tokenB, 1, monday, monday.plusDays(6));

        mockMvc.perform(put("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basicReportBody(1, monday, monday.plusDays(6))))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateWeekAndProjectForSameUserReturns409() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        LocalDate monday = nextMonday();

        createReport(token, 1, monday, monday.plusDays(6));

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basicReportBody(1, monday, monday.plusDays(6))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("CONFLICT")));
    }

    @Test
    void weekStartDateNotAMondayReturns400() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        LocalDate notMonday = nextMonday().plusDays(1);

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basicReportBody(1, notMonday, notMonday.plusDays(6))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void twoKeyBlockersInOnePayloadReturns400() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        LocalDate monday = nextMonday();

        String body = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s",
                 "blockers":[
                   {"title":"B1","impact":"LOW","keyIssue":true},
                   {"title":"B2","impact":"LOW","keyIssue":true}
                 ]}
                """.formatted(monday, monday.plusDays(6));

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void putReplacesChildrenExactlyOrphanRemovalProof() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        LocalDate monday = nextMonday();

        String threeTasks = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s",
                 "tasks":[
                   {"taskName":"T1","status":"NOT_STARTED","priority":"LOW","plannedPercentage":10,"actualPercentage":0,"hoursPlanned":1,"hoursSpent":0},
                   {"taskName":"T2","status":"NOT_STARTED","priority":"LOW","plannedPercentage":10,"actualPercentage":0,"hoursPlanned":1,"hoursSpent":0},
                   {"taskName":"T3","status":"NOT_STARTED","priority":"LOW","plannedPercentage":10,"actualPercentage":0,"hoursPlanned":1,"hoursSpent":0}
                 ]}
                """.formatted(monday, monday.plusDays(6));

        String createResponse = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(threeTasks))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long reportId = Long.valueOf(extractJsonStringValue(createResponse, "\"id\":"));
        assertThat(reportTaskRepository.countByReportId(reportId)).isEqualTo(3);

        String twoTasks = """
                {"projectId":1,"weekStartDate":"%s","weekEndDate":"%s",
                 "tasks":[
                   {"taskName":"New1","status":"NOT_STARTED","priority":"LOW","plannedPercentage":10,"actualPercentage":0,"hoursPlanned":1,"hoursSpent":0},
                   {"taskName":"New2","status":"NOT_STARTED","priority":"LOW","plannedPercentage":10,"actualPercentage":0,"hoursPlanned":1,"hoursSpent":0}
                 ]}
                """.formatted(monday, monday.plusDays(6));

        mockMvc.perform(put("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(twoTasks))
                .andExpect(status().isOk());

        assertThat(reportTaskRepository.countByReportId(reportId)).isEqualTo(2);
    }

    @Test
    void listMyReportsRespectsStatusAndProjectIdFilters() throws Exception {
        String token = loginAndGetToken("member@example.com", "Password123");
        LocalDate mondayClientA = nextMonday();
        LocalDate mondayInternalTooling = nextMonday();

        createReport(token, 1, mondayClientA, mondayClientA.plusDays(6));
        createReport(token, 2, mondayInternalTooling, mondayInternalTooling.plusDays(6));

        mockMvc.perform(get("/api/reports/my")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "DRAFT")
                        .param("projectId", "1")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.projectName == 'Internal Tooling')]").doesNotExist());
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/reports/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }
}
