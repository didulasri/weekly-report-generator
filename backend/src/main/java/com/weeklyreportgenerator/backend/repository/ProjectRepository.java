package com.weeklyreportgenerator.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreportgenerator.backend.entity.Project;
import com.weeklyreportgenerator.backend.entity.enums.ProjectStatus;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByName(String name);

    List<Project> findByStatus(ProjectStatus status);
}
