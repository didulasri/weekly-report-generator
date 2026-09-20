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

    @Query("""
            SELECT up FROM UserProject up
            JOIN FETCH up.project
            WHERE up.user.id = :userId AND up.active = true
            """)
    List<UserProject> findActiveWithProjectByUserId(@Param("userId") Long userId);

    // One grouped query for every user on the admin list page, instead of one count per row.
    @Query("SELECT up.user.id AS userId, COUNT(up) AS cnt "
            + "FROM UserProject up WHERE up.user.id IN :userIds AND up.active = true GROUP BY up.user.id")
    List<UserProjectCount> countActiveGroupedByUser(@Param("userIds") List<Long> userIds);

    interface UserProjectCount {
        Long getUserId();
        Long getCnt();
    }
}
