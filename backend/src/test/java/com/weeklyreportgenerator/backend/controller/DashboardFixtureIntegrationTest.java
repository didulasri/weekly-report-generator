package com.weeklyreportgenerator.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import jakarta.persistence.EntityManagerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

// One deterministic fixture (2 fresh projects, 5 fresh members, 4 reports covering all four
// statuses, known task/work-hour/blocker counts) backs every numeric assertion in this class, so
// every count below is a fact this test controls, not an assumption about pre-existing seed data.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class DashboardFixtureIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    // A week whose due date (Monday of the following week, 23:59:59 UTC) is long past by the time
    // this test runs -- so a report submitted "now" during the test is naturally LATE, with no
    // need to fake a future submittedAt.
    private static final LocalDate WEEK_START = LocalDate.of(2020, 1, 6);
    private static final LocalDate EMPTY_WEEK = LocalDate.of(2099, 1, 5);

    private String loginAndGetToken(String email, String password) throws Exception {
        return extractJsonStringValue(loginRaw(email, password), "\"accessToken\":\"");
    }

    private String loginRaw(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
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

    private String managerToken() throws Exception {
        return loginAndGetToken("manager@example.com", "Password123");
    }

    private void register(String name, String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","password":"Fixture123"}
                                """.formatted(name, email)))
                .andExpect(status().isCreated());
    }

    private Long createProject(String managerToken, String name) throws Exception {
        String response = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.valueOf(extractJsonStringValue(response, "\"id\":"));
    }

    private void assignMember(String managerToken, Long projectId, Long userId) throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":%d}".formatted(userId)))
                .andExpect(status().isCreated());
    }

    private Long createReport(String memberToken, Long projectId, String body) throws Exception {
        String response = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.valueOf(extractJsonStringValue(response, "\"id\":"));
    }

    private void submit(String memberToken, Long reportId) throws Exception {
        mockMvc.perform(post("/api/reports/" + reportId + "/submit").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());
    }

    private JsonNode getJson(String url, String token) throws Exception {
        String response = mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    @Test
    void teamMemberForbiddenOnEveryDashboardEndpoint() throws Exception {
        String memberToken = loginAndGetToken("member@example.com", "Password123");
        List<String> endpoints = List.of(
                "/api/dashboard/summary",
                "/api/dashboard/status-summary",
                "/api/dashboard/task-trends",
                "/api/dashboard/workload",
                "/api/dashboard/time-distribution",
                "/api/dashboard/activity-feed",
                "/api/dashboard/section-comparison?weekStart=2020-01-06&section=BLOCKERS");

        for (String endpoint : endpoints) {
            mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + memberToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void unauthenticatedOnEveryDashboardEndpoint() throws Exception {
        List<String> endpoints = List.of(
                "/api/dashboard/summary",
                "/api/dashboard/status-summary",
                "/api/dashboard/task-trends",
                "/api/dashboard/workload",
                "/api/dashboard/time-distribution",
                "/api/dashboard/activity-feed",
                "/api/dashboard/section-comparison?weekStart=2020-01-06&section=BLOCKERS");

        for (String endpoint : endpoints) {
            mockMvc.perform(get(endpoint)).andExpect(status().isUnauthorized());
        }
    }

    // Guards against a regression back to per-counter queries: the summary endpoint should stay at
    // 3 round trips (status GROUP BY, blocker FILTER, compliance FILTER) regardless of team size --
    // single digits, generously bounded at <10 so small legitimate additions don't make this flaky.
    @Test
    void summaryEndpointStaysWithinSingleDigitQueryCount() throws Exception {
        String managerToken = managerToken();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        mockMvc.perform(get("/api/dashboard/summary?weekStart=2020-01-06")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());

        long queryCount = statistics.getQueryExecutionCount();
        assertThat(queryCount).isLessThan(10);
    }

    @Test
    void complianceRateIsZeroNotDivideByZeroWhenNothingIsExpected() throws Exception {
        String managerToken = managerToken();
        // A projectId with no assignments at all -> expected == 0.
        JsonNode summary = getJson(
                "/api/dashboard/summary?weekStart=2020-01-06&projectId=999999", managerToken);

        assertThat(summary.get("compliance").get("expected").asLong()).isZero();
        assertThat(summary.get("compliance").get("ratePercent").asDouble()).isEqualTo(0.0);
        assertThat(summary.get("totalReportsThisWeek").asLong()).isZero();
    }

    @Test
    void emptyRangeReturnsZeroedOrEmptyResultsNeverNullNever500() throws Exception {
        String managerToken = managerToken();

        JsonNode summary = getJson(
                "/api/dashboard/summary?weekStart=" + EMPTY_WEEK + "&projectId=999999", managerToken);
        assertThat(summary.get("totalReportsThisWeek").asLong()).isZero();
        assertThat(summary.get("openBlockersCount").asLong()).isZero();

        JsonNode trends = getJson(
                "/api/dashboard/task-trends?startDate=" + EMPTY_WEEK + "&endDate=" + EMPTY_WEEK.plusDays(6)
                        + "&projectId=999999",
                managerToken);
        assertThat(trends.isArray()).isTrue();
        assertThat(trends).hasSize(1);
        assertThat(trends.get(0).get("totalTasks").asLong()).isZero();

        JsonNode timeDistribution = getJson(
                "/api/dashboard/time-distribution?startDate=" + EMPTY_WEEK + "&endDate=" + EMPTY_WEEK.plusDays(6)
                        + "&projectId=999999",
                managerToken);
        assertThat(timeDistribution.isArray()).isTrue();
        assertThat(timeDistribution).isEmpty();

        JsonNode activityFeed = getJson("/api/dashboard/activity-feed?page=0&size=5", managerToken);
        assertThat(activityFeed.get("content").isArray()).isTrue();
    }

    @Test
    void dashboardMetricsMatchDeterministicFixtureExactly() throws Exception {
        String managerToken = managerToken();

        Long project1 = createProject(managerToken, "Fixture Project Alpha " + Instant.now().toEpochMilli());
        Long project2 = createProject(managerToken, "Fixture Project Beta " + Instant.now().toEpochMilli());

        String[] emails = new String[5];
        Long[] userIds = new Long[5];
        String[] tokens = new String[5];
        for (int i = 0; i < 5; i++) {
            emails[i] = "fixture" + (i + 1) + "-" + Instant.now().toEpochMilli() + "@example.com";
            register("Fixture Member " + (i + 1), emails[i]);
            tokens[i] = loginAndGetToken(emails[i], "Fixture123");
            String me = loginRaw(emails[i], "Fixture123");
            userIds[i] = Long.valueOf(extractJsonStringValue(me, "\"id\":"));
            assignMember(managerToken, project1, userIds[i]);
        }

        String weekEnd = WEEK_START.plusDays(6).toString();

        // member1: DRAFT, never submitted. 1 task, not completed, hoursSpent=5. workHours DEVELOPMENT=10.
        String member1Body = """
                {"projectId":%d,"weekStartDate":"%s","weekEndDate":"%s","summary":"Fixture draft",
                 "tasks":[{"taskName":"T1","status":"IN_PROGRESS","priority":"MEDIUM","plannedPercentage":50,
                           "actualPercentage":50,"hoursPlanned":5,"hoursSpent":5}],
                 "workHours":[{"taskType":"DEVELOPMENT","hours":10}]}
                """.formatted(project1, WEEK_START, weekEnd);
        createReport(tokens[0], project1, member1Body);

        // member2: SUBMITTED, submitted "now" -> LATE (2020 due date is long past). 2 tasks (1 completed,
        // hoursSpent 6+4=10). workHours DEVELOPMENT=6, TESTING=4. 1 OPEN key blocker (a report's single
        // blocker must be the key one -- "exactly one key blocker when blockers are present" is enforced
        // at submit time, so a lone non-key blocker would fail completeness validation).
        String member2Body = """
                {"projectId":%d,"weekStartDate":"%s","weekEndDate":"%s","summary":"Fixture submitted",
                 "tasks":[{"taskName":"T1","status":"COMPLETED","priority":"HIGH","plannedPercentage":100,
                           "actualPercentage":100,"hoursPlanned":6,"hoursSpent":6},
                          {"taskName":"T2","status":"IN_PROGRESS","priority":"MEDIUM","plannedPercentage":40,
                           "actualPercentage":40,"hoursPlanned":4,"hoursSpent":4}],
                 "workHours":[{"taskType":"DEVELOPMENT","hours":6},{"taskType":"TESTING","hours":4}],
                 "blockers":[{"title":"Open key blocker","impact":"MEDIUM","status":"OPEN","keyIssue":true}]}
                """.formatted(project1, WEEK_START, weekEnd);
        Long report2 = createReport(tokens[1], project1, member2Body);
        submit(tokens[1], report2);

        // member3: submitted, then forced back to inside the due window -> ON_TIME, then APPROVED.
        // 1 completed task, hoursSpent=8. workHours DEVELOPMENT=10. 1 RESOLVED (not open) key blocker --
        // RESOLVED means it doesn't count toward openBlockersCount/keyBlockersCount despite keyFlag=true.
        String member3Body = """
                {"projectId":%d,"weekStartDate":"%s","weekEndDate":"%s","summary":"Fixture approved",
                 "tasks":[{"taskName":"T1","status":"COMPLETED","priority":"HIGH","plannedPercentage":100,
                           "actualPercentage":100,"hoursPlanned":8,"hoursSpent":8}],
                 "workHours":[{"taskType":"DEVELOPMENT","hours":10}],
                 "blockers":[{"title":"Resolved blocker","impact":"LOW","status":"RESOLVED","keyIssue":true}]}
                """.formatted(project1, WEEK_START, weekEnd);
        Long report3 = createReport(tokens[2], project1, member3Body);
        submit(tokens[2], report3);
        jdbcTemplate.update(
                "UPDATE weekly_reports SET submitted_at = ? WHERE id = ?",
                Timestamp.from(WEEK_START.plusDays(2).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()), report3);
        mockMvc.perform(post("/api/manager/reports/" + report3 + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Approved\"}"))
                .andExpect(status().isOk());

        // member4: submitted "now" -> LATE, then sent back -> NEEDS_CORRECTION. 1 non-completed task,
        // hoursSpent=4. workHours MEETINGS=10. Two OPEN blockers, the key one inserted SECOND, to prove
        // the key-flagged item is ordered first regardless of insertion order.
        String member4Body = """
                {"projectId":%d,"weekStartDate":"%s","weekEndDate":"%s","summary":"Fixture needs correction",
                 "tasks":[{"taskName":"T1","status":"IN_PROGRESS","priority":"LOW","plannedPercentage":30,
                           "actualPercentage":30,"hoursPlanned":4,"hoursSpent":4}],
                 "workHours":[{"taskType":"MEETINGS","hours":10}],
                 "blockers":[{"title":"Non-key blocker","impact":"LOW","status":"OPEN","keyIssue":false},
                             {"title":"Key blocker","impact":"HIGH","status":"OPEN","keyIssue":true}]}
                """.formatted(project1, WEEK_START, weekEnd);
        Long report4 = createReport(tokens[3], project1, member4Body);
        submit(tokens[3], report4);
        mockMvc.perform(post("/api/manager/reports/" + report4 + "/request-changes")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Please add more detail here please.\"}"))
                .andExpect(status().isOk());

        // member5: no report at all -> NOT_STARTED / PENDING.

        // ---------- summary ----------
        JsonNode summary = getJson(
                "/api/dashboard/summary?weekStart=" + WEEK_START + "&projectId=" + project1, managerToken);
        assertThat(summary.get("totalReportsThisWeek").asLong()).isEqualTo(4);
        assertThat(summary.get("draftCount").asLong()).isEqualTo(1);
        assertThat(summary.get("submittedCount").asLong()).isEqualTo(1);
        assertThat(summary.get("approvedCount").asLong()).isEqualTo(1);
        assertThat(summary.get("needsCorrectionCount").asLong()).isEqualTo(1);
        assertThat(summary.get("notStartedCount").asLong()).isEqualTo(1);
        // member2's key blocker (1) + member4's two blockers (1 non-key, 1 key) = 3 open; of those,
        // the key ones are member2's + member4's = 2 (member3's key blocker is RESOLVED, not counted).
        assertThat(summary.get("openBlockersCount").asLong()).isEqualTo(3);
        assertThat(summary.get("keyBlockersCount").asLong()).isEqualTo(2);

        JsonNode compliance = summary.get("compliance");
        assertThat(compliance.get("expected").asLong()).isEqualTo(5);
        assertThat(compliance.get("onTime").asLong()).isEqualTo(1);
        assertThat(compliance.get("late").asLong()).isEqualTo(2);
        assertThat(compliance.get("pending").asLong()).isEqualTo(2);
        assertThat(compliance.get("ratePercent").asDouble()).isEqualTo(60.0);

        // ---------- status-summary ----------
        JsonNode statusSummary = getJson(
                "/api/dashboard/status-summary?weekStart=" + WEEK_START + "&projectId=" + project1, managerToken);
        assertThat(statusSummary.get("statusCounts").get("DRAFT").asLong()).isEqualTo(1);
        assertThat(statusSummary.get("statusCounts").get("SUBMITTED").asLong()).isEqualTo(1);
        assertThat(statusSummary.get("statusCounts").get("NEEDS_CORRECTION").asLong()).isEqualTo(1);
        assertThat(statusSummary.get("statusCounts").get("APPROVED").asLong()).isEqualTo(1);

        long notStartedInBreakdown = 0;
        for (JsonNode member : statusSummary.get("members")) {
            notStartedInBreakdown += member.get("notStarted").asLong();
        }
        assertThat(notStartedInBreakdown).isEqualTo(1);

        // ---------- task-trends: gap-free window around the fixture week ----------
        JsonNode trends = getJson(
                "/api/dashboard/task-trends?startDate=" + WEEK_START.minusWeeks(1) + "&endDate="
                        + WEEK_START.plusWeeks(1) + "&projectId=" + project1,
                managerToken);
        assertThat(trends).hasSize(3);
        assertThat(trends.get(0).get("weekStartDate").stringValue()).isEqualTo(WEEK_START.minusWeeks(1).toString());
        assertThat(trends.get(1).get("weekStartDate").stringValue()).isEqualTo(WEEK_START.toString());
        assertThat(trends.get(2).get("weekStartDate").stringValue()).isEqualTo(WEEK_START.plusWeeks(1).toString());
        assertThat(trends.get(1).get("completedTasks").asLong()).isEqualTo(2);
        assertThat(trends.get(1).get("totalTasks").asLong()).isEqualTo(5);
        assertThat(trends.get(1).get("totalHoursSpent").asDouble()).isEqualTo(27.0);
        assertThat(trends.get(0).get("totalTasks").asLong()).isZero();
        assertThat(trends.get(2).get("totalTasks").asLong()).isZero();

        // ---------- time-distribution: exact hours and percentages summing to 100 ----------
        JsonNode timeDistribution = getJson(
                "/api/dashboard/time-distribution?startDate=" + WEEK_START + "&endDate=" + weekEnd
                        + "&projectId=" + project1,
                managerToken);
        double percentSum = 0;
        for (JsonNode row : timeDistribution) {
            percentSum += row.get("percentOfTotal").asDouble();
            switch (row.get("taskType").stringValue()) {
                case "DEVELOPMENT" -> assertThat(row.get("totalHours").asDouble()).isEqualTo(26.0);
                case "TESTING" -> assertThat(row.get("totalHours").asDouble()).isEqualTo(4.0);
                case "MEETINGS" -> assertThat(row.get("totalHours").asDouble()).isEqualTo(10.0);
                default -> throw new AssertionError("Unexpected taskType: " + row.get("taskType"));
            }
        }
        assertThat(timeDistribution).hasSize(3);
        assertThat(percentSum).isEqualTo(100.0);

        // ---------- workload: project1 (active) outranks project2 (zero activity) ----------
        JsonNode workload = getJson(
                "/api/dashboard/workload?startDate=" + WEEK_START + "&endDate=" + weekEnd, managerToken);
        int indexProject1 = -1;
        int indexProject2 = -1;
        for (int i = 0; i < workload.size(); i++) {
            long id = workload.get(i).get("projectId").asLong();
            if (id == project1) {
                indexProject1 = i;
                assertThat(workload.get(i).get("reportCount").asLong()).isEqualTo(4);
                assertThat(workload.get(i).get("taskCount").asLong()).isEqualTo(5);
                assertThat(workload.get(i).get("totalHoursSpent").asDouble()).isEqualTo(27.0);
                assertThat(workload.get(i).get("memberCount").asLong()).isEqualTo(4);
            } else if (id == project2) {
                indexProject2 = i;
                assertThat(workload.get(i).get("reportCount").asLong()).isZero();
                assertThat(workload.get(i).get("totalHoursSpent").asDouble()).isZero();
            }
        }
        assertThat(indexProject1).isGreaterThanOrEqualTo(0);
        assertThat(indexProject2).isGreaterThanOrEqualTo(0);
        assertThat(indexProject1).isLessThan(indexProject2);

        // ---------- activity-feed: fixture events appear in true descending time order ----------
        List<Long> fixtureReportIds = List.of(report2, report3, report4);
        List<Instant> occurredTimes = new ArrayList<>();
        int page = 0;
        while (true) {
            JsonNode feedPage = getJson("/api/dashboard/activity-feed?page=" + page + "&size=50", managerToken);
            for (JsonNode entry : feedPage.get("content")) {
                if (fixtureReportIds.contains(entry.get("reportId").asLong())) {
                    occurredTimes.add(Instant.parse(entry.get("occurredAt").stringValue()));
                }
            }
            if (feedPage.get("last").asBoolean()) {
                break;
            }
            page++;
        }
        assertThat(occurredTimes).hasSizeGreaterThanOrEqualTo(4); // >=2 submissions + >=2 review actions
        List<Instant> sortedDescending = occurredTimes.stream().sorted((a, b) -> b.compareTo(a)).toList();
        assertThat(occurredTimes).isEqualTo(sortedDescending);

        // ---------- section-comparison: member5 has no report -> NOT_STARTED, empty items ----------
        JsonNode sectionComparison = getJson(
                "/api/dashboard/section-comparison?weekStart=" + WEEK_START + "&section=BLOCKERS&projectId="
                        + project1,
                managerToken);
        boolean foundMember5NotStarted = false;
        boolean foundMember4KeyFirst = false;
        for (JsonNode row : sectionComparison) {
            if (row.get("userId").asLong() == userIds[4]) {
                assertThat(row.get("reportStatus").stringValue()).isEqualTo("NOT_STARTED");
                assertThat(row.get("items")).isEmpty();
                foundMember5NotStarted = true;
            }
            if (row.get("userId").asLong() == userIds[3]) {
                assertThat(row.get("items")).hasSize(2);
                assertThat(row.get("items").get(0).get("keyFlag").asBoolean()).isTrue();
                foundMember4KeyFirst = true;
            }
        }
        assertThat(foundMember5NotStarted).isTrue();
        assertThat(foundMember4KeyFirst).isTrue();
    }
}
