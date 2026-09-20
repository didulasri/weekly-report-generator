package com.weeklyreportgenerator.backend.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreportgenerator.backend.dto.request.AchievementRequest;
import com.weeklyreportgenerator.backend.dto.request.BlockerRequest;
import com.weeklyreportgenerator.backend.dto.request.CreateReportRequest;
import com.weeklyreportgenerator.backend.dto.request.NextWeekTaskRequest;
import com.weeklyreportgenerator.backend.dto.request.TaskRequest;
import com.weeklyreportgenerator.backend.dto.request.UpdateReportRequest;
import com.weeklyreportgenerator.backend.dto.request.WorkHourRequest;
import com.weeklyreportgenerator.backend.dto.response.ReportDetailResponse;
import com.weeklyreportgenerator.backend.entity.Achievement;
import com.weeklyreportgenerator.backend.entity.Blocker;
import com.weeklyreportgenerator.backend.entity.NextWeekTask;
import com.weeklyreportgenerator.backend.entity.Project;
import com.weeklyreportgenerator.backend.entity.ReportReview;
import com.weeklyreportgenerator.backend.entity.ReportTask;
import com.weeklyreportgenerator.backend.entity.ReportVersion;
import com.weeklyreportgenerator.backend.entity.User;
import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.entity.WorkHour;
import com.weeklyreportgenerator.backend.entity.enums.BlockerStatus;
import com.weeklyreportgenerator.backend.entity.enums.Priority;
import com.weeklyreportgenerator.backend.entity.enums.ProjectStatus;
import com.weeklyreportgenerator.backend.entity.enums.ReportStatus;
import com.weeklyreportgenerator.backend.entity.enums.RoleName;
import com.weeklyreportgenerator.backend.exception.DuplicateResourceException;
import com.weeklyreportgenerator.backend.exception.InvalidRequestException;
import com.weeklyreportgenerator.backend.exception.InvalidStatusTransitionException;
import com.weeklyreportgenerator.backend.exception.ReportSubmissionValidationException;
import com.weeklyreportgenerator.backend.exception.ResourceNotFoundException;
import com.weeklyreportgenerator.backend.repository.ProjectRepository;
import com.weeklyreportgenerator.backend.repository.ReportReviewRepository;
import com.weeklyreportgenerator.backend.repository.ReportTaskRepository;
import com.weeklyreportgenerator.backend.repository.ReportVersionRepository;
import com.weeklyreportgenerator.backend.repository.UserRepository;
import com.weeklyreportgenerator.backend.repository.WeeklyReportRepository;
import com.weeklyreportgenerator.backend.repository.WeeklyReportSpecifications;
import com.weeklyreportgenerator.backend.repository.WorkHourRepository;
import com.weeklyreportgenerator.backend.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ReportTaskRepository reportTaskRepository;
    private final WorkHourRepository workHourRepository;
    private final ReportVersionRepository reportVersionRepository;
    private final ReportReviewRepository reportReviewRepository;
    private final ReportWorkflowService reportWorkflowService;
    private final ReportSnapshotService reportSnapshotService;

    @Transactional
    public WeeklyReport createReport(CreateReportRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        validateWeekDates(request.getWeekStartDate(), request.getWeekEndDate());
        Project project = getActiveProjectOrThrow(request.getProjectId());

        if (weeklyReportRepository.existsByUserIdAndProjectIdAndWeekStartDate(
                userId, project.getId(), request.getWeekStartDate())) {
            throw new DuplicateResourceException(
                    "A report for this project and week already exists");
        }

        validateChildLists(request.getBlockers(), request.getAchievements(), request.getWorkHours());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        WeeklyReport report = WeeklyReport.builder()
                .user(user)
                .project(project)
                .weekStartDate(request.getWeekStartDate())
                .weekEndDate(request.getWeekEndDate())
                .summary(request.getSummary())
                .notes(request.getNotes())
                .build();

        applyChildren(report, request.getTasks(), request.getNextWeekTasks(),
                request.getBlockers(), request.getAchievements(), request.getWorkHours());

        return weeklyReportRepository.save(report);
    }

    @Transactional
    public WeeklyReport updateReport(Long id, UpdateReportRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        WeeklyReport report = weeklyReportRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));

        reportWorkflowService.assertEditable(report);
        assertIdentityUnchangedOnceSubmitted(report, request);

        validateWeekDates(request.getWeekStartDate(), request.getWeekEndDate());
        Project project = getActiveProjectOrThrow(request.getProjectId());

        if (weeklyReportRepository.existsByUserIdAndProjectIdAndWeekStartDateAndIdNot(
                userId, project.getId(), request.getWeekStartDate(), id)) {
            throw new DuplicateResourceException(
                    "A report for this project and week already exists");
        }

        validateChildLists(request.getBlockers(), request.getAchievements(), request.getWorkHours());

        report.setProject(project);
        report.setWeekStartDate(request.getWeekStartDate());
        report.setWeekEndDate(request.getWeekEndDate());
        report.setSummary(request.getSummary());
        report.setNotes(request.getNotes());

        report.clearTasks();
        report.clearNextWeekTasks();
        report.clearBlockers();
        report.clearAchievements();
        report.clearWorkHours();

        applyChildren(report, request.getTasks(), request.getNextWeekTasks(),
                request.getBlockers(), request.getAchievements(), request.getWorkHours());

        return weeklyReportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public WeeklyReport getReport(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        WeeklyReport report = weeklyReportRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
        initializeChildren(report);
        return report;
    }

    @Transactional(readOnly = true)
    public List<ReportReview> getReviewHistory(Long reportId) {
        Long userId = SecurityUtils.getCurrentUserId();
        assertOwnedByCurrentUser(reportId, userId);
        return reportReviewRepository.findByReportIdOrderByReviewedAtDesc(reportId);
    }

    // Idempotent by design -- acknowledging an already-acknowledged review is a no-op, not an error.
    @Transactional
    public void acknowledgeReview(Long reportId, Long reviewId) {
        Long userId = SecurityUtils.getCurrentUserId();
        WeeklyReport report = weeklyReportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));

        ReportReview review = reportReviewRepository.findById(reviewId)
                .filter(r -> r.getReport().getId().equals(report.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review " + reviewId + " not found for report " + reportId));

        if (review.getAcknowledgedAt() == null) {
            review.setAcknowledgedAt(Instant.now());
            reportReviewRepository.save(review);
        }
    }

    // Reuses the reviewer-fetch-joined query so callers can safely read reviewer.getName()
    // outside this method's transaction, same as getReviewHistory.
    @Transactional(readOnly = true)
    public Optional<ReportReview> latestReview(Long reportId) {
        return reportReviewRepository.findByReportIdOrderByReviewedAtDesc(reportId).stream().findFirst();
    }

    // Owner-scoped correction queue: reuses the existing hasUserId/hasStatus/fetchProject
    // specifications rather than a new query builder. Sort is applied by the caller's Pageable --
    // ordering by updatedAt DESC works because entering NEEDS_CORRECTION always comes from a
    // manager's review save, which bumps the report's @LastModifiedDate at that exact moment.
    @Transactional(readOnly = true)
    public Page<WeeklyReport> listNeedsCorrectionReports(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();

        Specification<WeeklyReport> spec = Specification
                .where(WeeklyReportSpecifications.hasUserId(userId))
                .and(WeeklyReportSpecifications.hasStatus(ReportStatus.NEEDS_CORRECTION))
                .and(WeeklyReportSpecifications.fetchProject());

        return weeklyReportRepository.findAll(spec, pageable);
    }

    // Hibernate can't join-fetch multiple List collections in one query (MultipleBagFetchException),
    // so each child collection is lazily initialized here individually, still inside the transaction,
    // before the entity is handed back to the mapper outside of it.
    private void initializeChildren(WeeklyReport report) {
        Hibernate.initialize(report.getProject());
        report.initializeChildCollections();
    }

    @Transactional(readOnly = true)
    public Page<WeeklyReport> listMyReports(
            ReportStatus status, Long projectId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();

        Specification<WeeklyReport> spec = Specification
                .where(WeeklyReportSpecifications.hasUserId(userId))
                .and(WeeklyReportSpecifications.hasStatus(status))
                .and(WeeklyReportSpecifications.hasProjectId(projectId))
                .and(WeeklyReportSpecifications.weekStartsOnOrAfter(startDate))
                .and(WeeklyReportSpecifications.weekStartsOnOrBefore(endDate))
                .and(WeeklyReportSpecifications.fetchProject());

        return weeklyReportRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public long countTasks(Long reportId) {
        return reportTaskRepository.countByReportId(reportId);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumWorkHours(Long reportId) {
        return workHourRepository.sumHoursByReportId(reportId);
    }

    @Transactional
    public void deleteReport(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();

        WeeklyReport report = weeklyReportRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));

        reportWorkflowService.assertDeletable(report);

        weeklyReportRepository.delete(report);
    }

    @Transactional
    public WeeklyReport submitReport(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();

        WeeklyReport report = weeklyReportRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));

        ReportStatus previousStatus = report.getStatus();

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Illegal for the current status -> InvalidStatusTransitionException -> 409.
        reportWorkflowService.transition(report, ReportAction.SUBMIT, actor);

        // Content incomplete -> ReportSubmissionValidationException -> 400 with every failure listed.
        validateCompleteness(report);

        if (previousStatus == ReportStatus.NEEDS_CORRECTION) {
            assertContentChangedSinceLastSubmission(report);
            report.setCurrentVersion(report.getCurrentVersion() + 1);
        }

        String snapshotJson = reportSnapshotService.serialize(report);
        ReportVersion version = ReportVersion.builder()
                .versionNumber(report.getCurrentVersion())
                .snapshotData(snapshotJson)
                .submittedAt(report.getSubmittedAt())
                .build();
        report.addVersion(version);

        return weeklyReportRepository.save(report);
    }

    // Only applied when the previous status was NEEDS_CORRECTION -- first submissions have no prior
    // version to compare against and are unaffected. Compares against the snapshot for the version
    // still in effect (report.getCurrentVersion() has not been incremented yet at this point).
    private void assertContentChangedSinceLastSubmission(WeeklyReport report) {
        ReportVersion lastVersion = reportVersionRepository
                .findByReportIdAndVersionNumber(report.getId(), report.getCurrentVersion())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + report.getCurrentVersion() + " not found for report " + report.getId()));

        if (reportSnapshotService.contentUnchangedSince(report, lastVersion.getSnapshotData())) {
            throw new InvalidStatusTransitionException(
                    "No changes were made since the manager's feedback -- edit the report before resubmitting");
        }
    }

    // MANAGER/ADMIN can read any report's versions; TEAM_MEMBER only their own. See canAccessReport --
    // the one place that branch lives, so it isn't duplicated between this and getVersionSnapshot.
    @Transactional(readOnly = true)
    public List<ReportVersion> listVersions(Long reportId) {
        loadAccessibleReport(reportId);
        return reportVersionRepository.findByReportIdOrderByVersionNumberDesc(reportId);
    }

    @Transactional(readOnly = true)
    public ReportDetailResponse getVersionSnapshot(Long reportId, Integer versionNumber) {
        loadAccessibleReport(reportId);

        ReportVersion version = reportVersionRepository.findByReportIdAndVersionNumber(reportId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " not found for report " + reportId));

        return reportSnapshotService.deserialize(version.getSnapshotData());
    }

    private void assertOwnedByCurrentUser(Long reportId, Long userId) {
        if (weeklyReportRepository.findByIdAndUserId(reportId, userId).isEmpty()) {
            throw new ResourceNotFoundException("Report not found: " + reportId);
        }
    }

    private WeeklyReport loadAccessibleReport(Long reportId) {
        Long userId = SecurityUtils.getCurrentUserId();
        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));
        User actor = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (!canAccessReport(report, actor)) {
            throw new ResourceNotFoundException("Report not found: " + reportId);
        }
        return report;
    }

    private boolean canAccessReport(WeeklyReport report, User actor) {
        RoleName role = actor.getRole().getName();
        return role == RoleName.MANAGER || role == RoleName.ADMIN
                || report.getUser().getId().equals(actor.getId());
    }

    private void validateCompleteness(WeeklyReport report) {
        List<String> errors = new ArrayList<>();

        List<ReportTask> tasks = report.getTasks();
        if (tasks.isEmpty()) {
            errors.add("At least one task is required before submitting");
        } else {
            for (int i = 0; i < tasks.size(); i++) {
                ReportTask task = tasks.get(i);
                if (task.getTaskName() == null || task.getTaskName().isBlank()) {
                    errors.add("tasks[" + i + "].taskName is required");
                }
                if (task.getStatus() == null) {
                    errors.add("tasks[" + i + "].status is required");
                }
                if (task.getPriority() == null) {
                    errors.add("tasks[" + i + "].priority is required");
                }
            }
        }

        if (report.getSummary() == null || report.getSummary().isBlank()) {
            errors.add("summary is required");
        }

        List<Blocker> blockers = report.getBlockers();
        if (!blockers.isEmpty()) {
            long keyIssueCount = blockers.stream().filter(Blocker::isKeyIssue).count();
            if (keyIssueCount != 1) {
                errors.add("Exactly one blocker must be marked as the key issue when blockers are present");
            }
        }

        List<Achievement> achievements = report.getAchievements();
        if (!achievements.isEmpty()) {
            long keyAchievementCount = achievements.stream().filter(Achievement::isKeyAchievement).count();
            if (keyAchievementCount != 1) {
                errors.add("Exactly one achievement must be marked as the key achievement when achievements are present");
            }
        }

        List<WorkHour> workHours = report.getWorkHours();
        if (!workHours.isEmpty()) {
            BigDecimal total = workHours.stream()
                    .map(WorkHour::getHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Total work hours must be greater than zero when work-hour rows are present");
            }
        }

        if (!errors.isEmpty()) {
            throw new ReportSubmissionValidationException(errors);
        }
    }

    // Once a report has been submitted at least once (currentVersion > 1, or status is
    // NEEDS_CORRECTION -- the first correction round is still version 1), the manager's review
    // history refers to "week X on project Y". Moving it to a different week/project would
    // silently invalidate that history, so project and week dates are locked from here on.
    private void assertIdentityUnchangedOnceSubmitted(WeeklyReport report, UpdateReportRequest request) {
        boolean locked = report.getCurrentVersion() > 1 || report.getStatus() == ReportStatus.NEEDS_CORRECTION;
        if (!locked) {
            return;
        }

        boolean projectChanged = !report.getProject().getId().equals(request.getProjectId());
        boolean weekChanged = !report.getWeekStartDate().equals(request.getWeekStartDate())
                || !report.getWeekEndDate().equals(request.getWeekEndDate());

        if (projectChanged || weekChanged) {
            throw new InvalidStatusTransitionException(
                    "Project and week dates cannot be changed once a report has been submitted -- "
                            + "the manager's review history refers to this report's original project and week");
        }
    }

    private void validateWeekDates(LocalDate weekStartDate, LocalDate weekEndDate) {
        if (weekEndDate.isBefore(weekStartDate)) {
            throw new InvalidRequestException("weekEndDate must not be before weekStartDate");
        }
        if (weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new InvalidRequestException("weekStartDate must be a Monday");
        }
        if (!weekEndDate.equals(weekStartDate.plusDays(6))) {
            throw new InvalidRequestException("weekEndDate must be the Sunday following weekStartDate");
        }
    }

    private Project getActiveProjectOrThrow(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new InvalidRequestException("Project " + projectId + " is not active");
        }
        return project;
    }

    private void validateChildLists(
            List<BlockerRequest> blockers, List<AchievementRequest> achievements, List<WorkHourRequest> workHours) {
        long keyIssueCount = blockers == null ? 0 : blockers.stream().filter(BlockerRequest::isKeyIssue).count();
        if (keyIssueCount > 1) {
            throw new InvalidRequestException("At most one blocker may be marked as the key issue");
        }

        long keyAchievementCount = achievements == null ? 0
                : achievements.stream().filter(AchievementRequest::isKeyAchievement).count();
        if (keyAchievementCount > 1) {
            throw new InvalidRequestException("At most one achievement may be marked as the key achievement");
        }

        if (workHours != null) {
            long distinctCategories = workHours.stream().map(WorkHourRequest::getTaskType).distinct().count();
            if (distinctCategories != workHours.size()) {
                throw new InvalidRequestException("Each work category may appear at most once per report");
            }
        }
    }

    private void applyChildren(
            WeeklyReport report,
            List<TaskRequest> tasks,
            List<NextWeekTaskRequest> nextWeekTasks,
            List<BlockerRequest> blockers,
            List<AchievementRequest> achievements,
            List<WorkHourRequest> workHours) {

        if (tasks != null) {
            tasks.forEach(t -> report.addTask(ReportTask.builder()
                    .taskName(t.getTaskName())
                    .description(t.getDescription())
                    .status(t.getStatus())
                    .priority(t.getPriority())
                    .plannedPercentage(t.getPlannedPercentage())
                    .actualPercentage(t.getActualPercentage())
                    .hoursPlanned(t.getHoursPlanned())
                    .hoursSpent(t.getHoursSpent())
                    .deliverable(t.getDeliverable())
                    .build()));
        }

        if (nextWeekTasks != null) {
            nextWeekTasks.forEach(t -> report.addNextWeekTask(NextWeekTask.builder()
                    .taskName(t.getTaskName())
                    .description(t.getDescription())
                    .priority(t.getPriority() != null ? t.getPriority() : Priority.MEDIUM)
                    .build()));
        }

        if (blockers != null) {
            blockers.forEach(b -> report.addBlocker(Blocker.builder()
                    .title(b.getTitle())
                    .description(b.getDescription())
                    .impact(b.getImpact())
                    .status(b.getStatus() != null ? b.getStatus() : BlockerStatus.OPEN)
                    .isKeyIssue(b.isKeyIssue())
                    .build()));
        }

        if (achievements != null) {
            achievements.forEach(a -> report.addAchievement(Achievement.builder()
                    .title(a.getTitle())
                    .description(a.getDescription())
                    .isKeyAchievement(a.isKeyAchievement())
                    .build()));
        }

        if (workHours != null) {
            workHours.forEach(w -> report.addWorkHour(WorkHour.builder()
                    .taskType(w.getTaskType())
                    .hours(w.getHours())
                    .build()));
        }
    }
}
