package com.weeklyreportgenerator.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreportgenerator.backend.dto.response.TeamMemberResponse;
import com.weeklyreportgenerator.backend.entity.ReportReview;
import com.weeklyreportgenerator.backend.entity.ReportVersion;
import com.weeklyreportgenerator.backend.entity.User;
import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.entity.enums.ReportStatus;
import com.weeklyreportgenerator.backend.entity.enums.ReviewAction;
import com.weeklyreportgenerator.backend.entity.enums.RoleName;
import com.weeklyreportgenerator.backend.exception.ResourceNotFoundException;
import com.weeklyreportgenerator.backend.repository.ReportReviewRepository;
import com.weeklyreportgenerator.backend.repository.ReportVersionRepository;
import com.weeklyreportgenerator.backend.repository.UserRepository;
import com.weeklyreportgenerator.backend.repository.WeeklyReportRepository;
import com.weeklyreportgenerator.backend.repository.WeeklyReportRepository.ReportStatusCount;
import com.weeklyreportgenerator.backend.repository.WeeklyReportSpecifications;
import com.weeklyreportgenerator.backend.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

// Manager-only reads and reviews. Deliberately separate from ReportService: this class never
// touches report content (tasks, summary, dates) -- it only reads reports and writes ReportReview
// rows, so there's no path here that could be pointed at the wrong DTO and mutate content by mistake.
@Service
@RequiredArgsConstructor
public class ManagerReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final UserRepository userRepository;
    private final ReportReviewRepository reportReviewRepository;
    private final ReportVersionRepository reportVersionRepository;
    private final ReportWorkflowService reportWorkflowService;

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional(readOnly = true)
    public Page<WeeklyReport> listTeamReports(
            ReportStatus status, Long projectId, Long userId,
            LocalDate startDate, LocalDate endDate, Pageable pageable) {

        Specification<WeeklyReport> spec = Specification
                .where(WeeklyReportSpecifications.hasStatus(status))
                .and(WeeklyReportSpecifications.hasProjectId(projectId))
                .and(WeeklyReportSpecifications.hasUserId(userId))
                .and(WeeklyReportSpecifications.weekStartsOnOrAfter(startDate))
                .and(WeeklyReportSpecifications.weekStartsOnOrBefore(endDate))
                .and(WeeklyReportSpecifications.fetchProject())
                .and(WeeklyReportSpecifications.fetchUser());

        return weeklyReportRepository.findAll(spec, pageable);
    }

    // No 404-on-other-owner rule here -- managers legitimately see every report by id.
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional(readOnly = true)
    public WeeklyReport getReportForReview(Long id) {
        WeeklyReport report = weeklyReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
        Hibernate.initialize(report.getProject());
        Hibernate.initialize(report.getUser());
        report.initializeChildCollections();
        return report;
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional(readOnly = true)
    public List<ReportReview> listReviews(Long reportId) {
        return reportReviewRepository.findByReportIdOrderByReviewedAtDesc(reportId);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional(readOnly = true)
    public List<ReportVersion> listVersions(Long reportId) {
        return reportVersionRepository.findByReportIdOrderByVersionNumberDesc(reportId);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional
    public WeeklyReport approveReport(Long id, String comment) {
        return applyReview(id, ReportAction.APPROVE, ReviewAction.APPROVED, comment);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional
    public WeeklyReport requestChanges(Long id, String comment) {
        return applyReview(id, ReportAction.REQUEST_CHANGES, ReviewAction.REQUEST_CHANGES, comment);
    }

    // The status change and its review row are written in one transaction -- a review must never
    // exist without the status change it documents, and vice versa.
    private WeeklyReport applyReview(Long reportId, ReportAction action, ReviewAction reviewAction, String comment) {
        Long actorId = SecurityUtils.getCurrentUserId();
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actorId));
        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));

        // Illegal for the current status -> InvalidStatusTransitionException -> 409.
        // Wrong actor role -> AccessDeniedException -> 403.
        reportWorkflowService.transition(report, action, actor);

        ReportReview review = ReportReview.builder()
                .reviewer(actor)
                .versionNumber(report.getCurrentVersion())
                .action(reviewAction)
                .comment(comment)
                .reviewedAt(Instant.now())
                .build();
        report.addReview(review);

        WeeklyReport saved = weeklyReportRepository.save(report);
        // The mapper reads tasks/blockers/etc. and project/user after this method returns, once the
        // Hibernate session is closed (open-in-view is off) -- initialize everything it needs now.
        Hibernate.initialize(saved.getProject());
        Hibernate.initialize(saved.getUser());
        saved.initializeChildCollections();
        return saved;
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> listTeamMembers(Pageable pageable) {
        Page<User> members = userRepository.findByRoleName(RoleName.TEAM_MEMBER, pageable);
        List<Long> memberIds = members.getContent().stream().map(User::getId).toList();

        Map<Long, Map<ReportStatus, Long>> countsByUser = new HashMap<>();
        if (!memberIds.isEmpty()) {
            for (ReportStatusCount row : weeklyReportRepository.countByStatusGroupedByUser(memberIds)) {
                countsByUser.computeIfAbsent(row.getUserId(), k -> new EnumMap<>(ReportStatus.class))
                        .put(row.getStatus(), row.getCnt());
            }
        }

        return members.getContent().stream()
                .map(member -> toTeamMemberResponse(member, countsByUser.getOrDefault(member.getId(), Map.of())))
                .toList();
    }

    private TeamMemberResponse toTeamMemberResponse(User member, Map<ReportStatus, Long> counts) {
        Map<String, Long> byStatus = new HashMap<>();
        for (ReportStatus status : ReportStatus.values()) {
            byStatus.put(status.name(), counts.getOrDefault(status, 0L));
        }
        return TeamMemberResponse.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .active(member.isActive())
                .reportCountsByStatus(byStatus)
                .build();
    }
}
