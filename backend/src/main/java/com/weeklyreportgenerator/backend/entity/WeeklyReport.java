package com.weeklyreportgenerator.backend.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Hibernate;

import com.weeklyreportgenerator.backend.entity.enums.ReportStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "weekly_reports")
public class WeeklyReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Setter(AccessLevel.NONE)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReportStatus status = ReportStatus.DRAFT;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "current_version", nullable = false)
    private Integer currentVersion = 1;

    @Setter(AccessLevel.NONE)
    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportTask> tasks = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NextWeekTask> nextWeekTasks = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Blocker> blockers = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Achievement> achievements = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkHour> workHours = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportReview> reviews = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportVersion> versions = new ArrayList<>();

    // Forces each lazy child collection to load while a session is still open. Must touch the raw
    // fields directly -- Hibernate.initialize() is a no-op on the unmodifiableList wrapper the
    // getters return, since that wrapper isn't a Hibernate PersistentCollection.
    public void initializeChildCollections() {
        Hibernate.initialize(tasks);
        Hibernate.initialize(nextWeekTasks);
        Hibernate.initialize(blockers);
        Hibernate.initialize(achievements);
        Hibernate.initialize(workHours);
    }

    public List<ReportTask> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public void addTask(ReportTask task) {
        tasks.add(task);
        task.setReport(this);
    }

    public void removeTask(ReportTask task) {
        tasks.remove(task);
        task.setReport(null);
    }

    public void clearTasks() {
        tasks.forEach(t -> t.setReport(null));
        tasks.clear();
    }

    public List<NextWeekTask> getNextWeekTasks() {
        return Collections.unmodifiableList(nextWeekTasks);
    }

    public void addNextWeekTask(NextWeekTask task) {
        nextWeekTasks.add(task);
        task.setReport(this);
    }

    public void removeNextWeekTask(NextWeekTask task) {
        nextWeekTasks.remove(task);
        task.setReport(null);
    }

    public void clearNextWeekTasks() {
        nextWeekTasks.forEach(t -> t.setReport(null));
        nextWeekTasks.clear();
    }

    public List<Blocker> getBlockers() {
        return Collections.unmodifiableList(blockers);
    }

    public void addBlocker(Blocker blocker) {
        blockers.add(blocker);
        blocker.setReport(this);
    }

    public void removeBlocker(Blocker blocker) {
        blockers.remove(blocker);
        blocker.setReport(null);
    }

    public void clearBlockers() {
        blockers.forEach(b -> b.setReport(null));
        blockers.clear();
    }

    public List<Achievement> getAchievements() {
        return Collections.unmodifiableList(achievements);
    }

    public void addAchievement(Achievement achievement) {
        achievements.add(achievement);
        achievement.setReport(this);
    }

    public void removeAchievement(Achievement achievement) {
        achievements.remove(achievement);
        achievement.setReport(null);
    }

    public void clearAchievements() {
        achievements.forEach(a -> a.setReport(null));
        achievements.clear();
    }

    public List<WorkHour> getWorkHours() {
        return Collections.unmodifiableList(workHours);
    }

    public void addWorkHour(WorkHour workHour) {
        workHours.add(workHour);
        workHour.setReport(this);
    }

    public void removeWorkHour(WorkHour workHour) {
        workHours.remove(workHour);
        workHour.setReport(null);
    }

    public void clearWorkHours() {
        workHours.forEach(w -> w.setReport(null));
        workHours.clear();
    }

    // The only way status/submittedAt ever change. Called exclusively by ReportWorkflowService --
    // that's the one-word answer to "where do status changes happen?"
    public void applyStatusChange(ReportStatus newStatus, Instant submittedAt) {
        this.status = newStatus;
        if (submittedAt != null) {
            this.submittedAt = submittedAt;
        }
    }

    public List<ReportVersion> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    public void addVersion(ReportVersion version) {
        versions.add(version);
        version.setReport(this);
    }

    public List<ReportReview> getReviews() {
        return Collections.unmodifiableList(reviews);
    }

    public void addReview(ReportReview review) {
        reviews.add(review);
        review.setReport(this);
    }
}
