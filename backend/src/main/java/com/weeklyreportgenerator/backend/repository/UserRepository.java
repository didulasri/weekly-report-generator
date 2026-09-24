package com.weeklyreportgenerator.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreportgenerator.backend.entity.User;
import com.weeklyreportgenerator.backend.entity.enums.RoleName;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByEmailIgnoreCaseAndActiveTrue(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.id = :id")
    Optional<User> findByIdWithRole(@Param("id") Long id);

    @Query("SELECT u FROM User u JOIN FETCH u.role r WHERE r.name = :roleName")
    Page<User> findByRoleName(@Param("roleName") RoleName roleName, Pageable pageable);

    // Single-query filtered/searched admin listing, role fetch-joined so mapping to UserResponse
    // never touches the lazy role association outside this query.
    @Query("""
            SELECT u FROM User u JOIN FETCH u.role r
            WHERE (:role IS NULL OR r.name = :role)
              AND (:active IS NULL OR u.active = :active)
              AND (:search IS NULL
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<User> search(
            @Param("role") RoleName role,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable);

    // Guard query for the last-active-admin checks (self-demotion/self-deactivation/last-admin).
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName AND u.active = true")
    long countByRoleNameAndActiveTrue(@Param("roleName") RoleName roleName);
}
