package com.weeklyreportgenerator.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreportgenerator.backend.dto.dashboard.ActivityFeedEntryResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.DashboardSummaryResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.SectionComparisonResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.SectionType;
import com.weeklyreportgenerator.backend.dto.dashboard.StatusSummaryResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.TaskTrendResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.TimeDistributionResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.WorkloadResponse;
import com.weeklyreportgenerator.backend.dto.response.PagedResponse;
import com.weeklyreportgenerator.backend.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) Long projectId) {
        return ResponseEntity.ok(dashboardService.getSummary(weekStart, projectId));
    }

    @GetMapping("/status-summary")
    public ResponseEntity<StatusSummaryResponse> getStatusSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) Long projectId) {
        return ResponseEntity.ok(dashboardService.getStatusSummary(weekStart, projectId));
    }

    @GetMapping("/task-trends")
    public ResponseEntity<List<TaskTrendResponse>> getTaskTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(dashboardService.getTaskTrends(startDate, endDate, projectId, userId));
    }

    @GetMapping("/workload")
    public ResponseEntity<List<WorkloadResponse>> getWorkload(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long projectId) {
        return ResponseEntity.ok(dashboardService.getWorkload(startDate, endDate, projectId));
    }

    @GetMapping("/time-distribution")
    public ResponseEntity<List<TimeDistributionResponse>> getTimeDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long projectId) {
        return ResponseEntity.ok(dashboardService.getTimeDistribution(startDate, endDate, projectId));
    }

    @GetMapping("/activity-feed")
    public ResponseEntity<PagedResponse<ActivityFeedEntryResponse>> getActivityFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(dashboardService.getActivityFeed(pageable));
    }

    @GetMapping("/section-comparison")
    public ResponseEntity<List<SectionComparisonResponse>> getSectionComparison(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam SectionType section,
            @RequestParam(required = false) Long projectId) {
        return ResponseEntity.ok(dashboardService.getSectionComparison(weekStart, section, projectId));
    }
}
