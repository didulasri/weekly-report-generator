package com.weeklyreportgenerator.backend.entity;

import java.math.BigDecimal;

import com.weeklyreportgenerator.backend.entity.enums.Priority;
import com.weeklyreportgenerator.backend.entity.enums.TaskStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "report_tasks")
public class ReportTask extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private WeeklyReport report;

    @Column(name = "task_name", nullable = false, length = 255)
    private String taskName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private Priority priority;

    @Column(name = "planned_percentage", nullable = false)
    private Integer plannedPercentage;

    @Column(name = "actual_percentage", nullable = false)
    private Integer actualPercentage;

    @Builder.Default
    @Column(name = "hours_planned", nullable = false, precision = 6, scale = 2)
    private BigDecimal hoursPlanned = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "hours_spent", nullable = false, precision = 6, scale = 2)
    private BigDecimal hoursSpent = BigDecimal.ZERO;

    @Column(name = "deliverable", length = 500)
    private String deliverable;
}
