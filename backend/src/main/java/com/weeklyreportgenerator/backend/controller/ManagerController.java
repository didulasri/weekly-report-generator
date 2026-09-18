package com.weeklyreportgenerator.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreportgenerator.backend.dto.request.ApproveRequest;
import com.weeklyreportgenerator.backend.dto.request.RequestChangesRequest;
import com.weeklyreportgenerator.backend.dto.response.ManagerReportDetailResponse;
import com.weeklyreportgenerator.backend.dto.response.ManagerReportSummaryResponse;
import com.weeklyreportgenerator.backend.dto.response.PagedResponse;
import com.weeklyreportgenerator.backend.dto.response.TeamMemberResponse;
import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.entity.enums.ReportStatus;
import com.weeklyreportgenerator.backend.mapper.ReportMapper;
import com.weeklyreportgenerator.backend.service.ManagerReportService;
import com.weeklyreportgenerator.backend.service.ReportService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final ManagerReportService managerReportService;
    private final ReportService reportService;
    private final ReportMapper reportMapper;

    @GetMapping("/reports")
    public ResponseEntity<PagedResponse<ManagerReportSummaryResponse>> listTeamReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<WeeklyReport> reports = managerReportService.listTeamReports(
                status, projectId, userId, startDate, endDate, pageable);

        Page<ManagerReportSummaryResponse> summaries = reports.map(report -> reportMapper.toManagerSummaryResponse(
                report,
                reportService.countTasks(report.getId()),
                reportService.sumWorkHours(report.getId())));

        return ResponseEntity.ok(PagedResponse.of(summaries));
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<ManagerReportDetailResponse> getReportForReview(@PathVariable Long id) {
        WeeklyReport report = managerReportService.getReportForReview(id);
        var reviews = managerReportService.listReviews(id);
        var versions = managerReportService.listVersions(id);
        return ResponseEntity.ok(reportMapper.toManagerDetailResponse(report, reviews, versions));
    }

    @GetMapping("/team-members")
    public ResponseEntity<List<TeamMemberResponse>> listTeamMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return ResponseEntity.ok(managerReportService.listTeamMembers(pageable));
    }

    @PostMapping("/reports/{id}/approve")
    public ResponseEntity<ManagerReportDetailResponse> approveReport(
            @PathVariable Long id, @RequestBody(required = false) ApproveRequest request) {
        String comment = request != null ? request.getComment() : null;
        WeeklyReport report = managerReportService.approveReport(id, comment);
        var reviews = managerReportService.listReviews(id);
        var versions = managerReportService.listVersions(id);
        return ResponseEntity.ok(reportMapper.toManagerDetailResponse(report, reviews, versions));
    }

    @PostMapping("/reports/{id}/request-changes")
    public ResponseEntity<ManagerReportDetailResponse> requestChanges(
            @PathVariable Long id, @Valid @RequestBody RequestChangesRequest request) {
        WeeklyReport report = managerReportService.requestChanges(id, request.getComment());
        var reviews = managerReportService.listReviews(id);
        var versions = managerReportService.listVersions(id);
        return ResponseEntity.ok(reportMapper.toManagerDetailResponse(report, reviews, versions));
    }
}
