package com.weeklyreportgenerator.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreportgenerator.backend.entity.Project;
import com.weeklyreportgenerator.backend.entity.enums.ProjectStatus;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByName(String name);

    List<Project> findByStatus(ProjectStatus status);

    boolean existsByNameIgnoreCase(String name);

    @Query("""
            SELECT p FROM Project p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<Project> findAll(@Param("status") ProjectStatus status, @Param("search") String search, Pageable pageable);
}
