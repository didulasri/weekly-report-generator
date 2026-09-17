package com.weeklyreportgenerator.backend.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreportgenerator.backend.dto.request.CreateReportRequest;
import com.weeklyreportgenerator.backend.dto.request.UpdateReportRequest;
import com.weeklyreportgenerator.backend.dto.response.PagedResponse;
import com.weeklyreportgenerator.backend.dto.response.ReportDetailResponse;
import com.weeklyreportgenerator.backend.dto.response.ReportSummaryResponse;
import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.entity.enums.ReportStatus;
import com.weeklyreportgenerator.backend.mapper.ReportMapper;
import com.weeklyreportgenerator.backend.service.ReportService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportMapper reportMapper;

    @PostMapping
    public ResponseEntity<ReportDetailResponse> createReport(@Valid @RequestBody CreateReportRequest request) {
        WeeklyReport created = reportService.createReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reportMapper.toDetailResponse(created));
    }

    @GetMapping("/my")
    public ResponseEntity<PagedResponse<ReportSummaryResponse>> listMyReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "weekStartDate"));
        Page<WeeklyReport> reports = reportService.listMyReports(status, projectId, startDate, endDate, pageable);

        Page<ReportSummaryResponse> summaries = reports.map(report -> reportMapper.toSummaryResponse(
                report,
                reportService.countTasks(report.getId()),
                reportService.sumWorkHours(report.getId())));

        return ResponseEntity.ok(PagedResponse.of(summaries));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportDetailResponse> getReport(@PathVariable Long id) {
        WeeklyReport report = reportService.getReport(id);
        return ResponseEntity.ok(reportMapper.toDetailResponse(report));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReportDetailResponse> updateReport(
            @PathVariable Long id, @Valid @RequestBody UpdateReportRequest request) {
        WeeklyReport updated = reportService.updateReport(id, request);
        return ResponseEntity.ok(reportMapper.toDetailResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}
