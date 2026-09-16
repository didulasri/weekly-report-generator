package com.weeklyreportgenerator.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreportgenerator.backend.entity.UserProject;

public interface UserProjectRepository extends JpaRepository<UserProject, Long> {

    List<UserProject> findByUserId(Long userId);

    List<UserProject> findByProjectId(Long projectId);

    Optional<UserProject> findByUserIdAndProjectId(Long userId, Long projectId);

    boolean existsByUserIdAndProjectId(Long userId, Long projectId);

    long countByProjectIdAndActiveTrue(Long projectId);

    @Query("""
            SELECT up FROM UserProject up
            JOIN FETCH up.user u
            JOIN FETCH u.role
            WHERE up.project.id = :projectId AND up.active = true
            """)
    List<UserProject> findActiveWithUserByProjectId(@Param("projectId") Long projectId);
}
