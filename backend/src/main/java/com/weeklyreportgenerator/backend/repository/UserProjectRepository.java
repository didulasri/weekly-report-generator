package com.weeklyreportgenerator.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreportgenerator.backend.entity.UserProject;

public interface UserProjectRepository extends JpaRepository<UserProject, Long> {

    List<UserProject> findByUserId(Long userId);

    List<UserProject> findByProjectId(Long projectId);

    Optional<UserProject> findByUserIdAndProjectId(Long userId, Long projectId);
}
