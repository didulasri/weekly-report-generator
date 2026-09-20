package com.weeklyreportgenerator.backend.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreportgenerator.backend.dto.request.CreateUserRequest;
import com.weeklyreportgenerator.backend.dto.request.UpdateUserRequest;
import com.weeklyreportgenerator.backend.dto.response.AdminUserProjectResponse;
import com.weeklyreportgenerator.backend.dto.response.ReportStatusBreakdown;
import com.weeklyreportgenerator.backend.dto.response.UserDetailResponse;
import com.weeklyreportgenerator.backend.dto.response.UserResponse;
import com.weeklyreportgenerator.backend.entity.Role;
import com.weeklyreportgenerator.backend.entity.User;
import com.weeklyreportgenerator.backend.entity.UserProject;
import com.weeklyreportgenerator.backend.entity.enums.RoleName;
import com.weeklyreportgenerator.backend.exception.DuplicateResourceException;
import com.weeklyreportgenerator.backend.exception.InvalidStatusTransitionException;
import com.weeklyreportgenerator.backend.exception.ResourceNotFoundException;
import com.weeklyreportgenerator.backend.repository.RoleRepository;
import com.weeklyreportgenerator.backend.repository.UserProjectRepository;
import com.weeklyreportgenerator.backend.repository.UserRepository;
import com.weeklyreportgenerator.backend.repository.WeeklyReportRepository;
import com.weeklyreportgenerator.backend.repository.WeeklyReportRepository.ReportStatusCount;
import com.weeklyreportgenerator.backend.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserProjectRepository userProjectRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(RoleName role, Boolean active, String search, Pageable pageable) {
        Page<User> users = userRepository.search(role, active, search, pageable);

        List<Long> userIds = users.getContent().stream().map(User::getId).toList();
        Map<Long, Long> projectCounts = groupBy(
                userProjectRepository.countActiveGroupedByUser(userIds),
                c -> c.getUserId(), c -> c.getCnt());
        Map<Long, Long> reportCounts = weeklyReportRepository.countByStatusGroupedByUser(userIds).stream()
                .collect(Collectors.groupingBy(ReportStatusCount::getUserId, Collectors.summingLong(ReportStatusCount::getCnt)));

        return users.map(user -> toResponse(user, projectCounts.getOrDefault(user.getId(), 0L),
                reportCounts.getOrDefault(user.getId(), 0L)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new IllegalStateException(request.getRole() + " role is not seeded"));

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .active(true)
                .build();

        user = userRepository.save(user);
        // A newly created account has no assignments or reports yet -- no need to query for zeros.
        return toResponse(user, 0L, 0L);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = getUserOrThrow(id);

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(request.getEmail(), id)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user = userRepository.save(user);

        long projectCount = userProjectRepository.findActiveWithProjectByUserId(id).size();
        long reportCount = weeklyReportRepository.countByStatusGroupedByUser(List.of(id)).stream()
                .mapToLong(ReportStatusCount::getCnt).sum();
        return toResponse(user, projectCount, reportCount);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long id) {
        User user = getUserOrThrow(id);

        List<UserProject> assignments = userProjectRepository.findActiveWithProjectByUserId(id);
        List<AdminUserProjectResponse> projects = assignments.stream()
                .map(up -> AdminUserProjectResponse.builder()
                        .id(up.getProject().getId())
                        .name(up.getProject().getName())
                        .status(up.getProject().getStatus())
                        .assignedAt(up.getAssignedAt())
                        .build())
                .toList();

        List<ReportStatusCount> statusCounts = weeklyReportRepository.countByStatusGroupedByUser(List.of(id));
        ReportStatusBreakdown breakdown = toBreakdown(statusCounts);
        long reportCount = statusCounts.stream().mapToLong(ReportStatusCount::getCnt).sum();

        return UserDetailResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .projectCount(projects.size())
                .reportCount(reportCount)
                .projects(projects)
                .reportStatusBreakdown(breakdown)
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse changeRole(Long id, RoleName newRole) {
        assertNotSelf(id, "change their own role");
        User user = getUserOrThrow(id);

        if (user.getRole().getName() == RoleName.ADMIN && newRole != RoleName.ADMIN) {
            assertNotLastActiveAdmin("demoted");
        }

        Role role = roleRepository.findByName(newRole)
                .orElseThrow(() -> new IllegalStateException(newRole + " role is not seeded"));
        user.setRole(role);
        user = userRepository.save(user);

        long projectCount = userProjectRepository.findActiveWithProjectByUserId(id).size();
        long reportCount = weeklyReportRepository.countByStatusGroupedByUser(List.of(id)).stream()
                .mapToLong(ReportStatusCount::getCnt).sum();
        return toResponse(user, projectCount, reportCount);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deactivateUser(Long id) {
        assertNotSelf(id, "deactivate their own account");
        User user = getUserOrThrow(id);

        if (user.getRole().getName() == RoleName.ADMIN) {
            assertNotLastActiveAdmin("deactivated");
        }

        user.setActive(false);
        userRepository.save(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse activateUser(Long id) {
        User user = getUserOrThrow(id);
        user.setActive(true);
        user = userRepository.save(user);

        long projectCount = userProjectRepository.findActiveWithProjectByUserId(id).size();
        long reportCount = weeklyReportRepository.countByStatusGroupedByUser(List.of(id)).stream()
                .mapToLong(ReportStatusCount::getCnt).sum();
        return toResponse(user, projectCount, reportCount);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = getUserOrThrow(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // Self-protection guards -- kept together here rather than scattered across endpoints, since
    // both role-change and deactivation share the exact same "don't lock the system out" logic.
    private void assertNotSelf(Long targetId, String action) {
        if (targetId.equals(SecurityUtils.getCurrentUserId())) {
            throw new InvalidStatusTransitionException("An admin cannot " + action);
        }
    }

    private void assertNotLastActiveAdmin(String action) {
        if (userRepository.countByRoleNameAndActiveTrue(RoleName.ADMIN) <= 1) {
            throw new InvalidStatusTransitionException("The last active admin cannot be " + action);
        }
    }

    User getUserOrThrow(Long id) {
        return userRepository.findByIdWithRole(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private UserResponse toResponse(User user, long projectCount, long reportCount) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .projectCount(projectCount)
                .reportCount(reportCount)
                .build();
    }

    private ReportStatusBreakdown toBreakdown(List<ReportStatusCount> counts) {
        ReportStatusBreakdown.ReportStatusBreakdownBuilder builder = ReportStatusBreakdown.builder();
        for (ReportStatusCount count : counts) {
            switch (count.getStatus()) {
                case DRAFT -> builder.draft(count.getCnt());
                case SUBMITTED -> builder.submitted(count.getCnt());
                case NEEDS_CORRECTION -> builder.needsCorrection(count.getCnt());
                case APPROVED -> builder.approved(count.getCnt());
            }
        }
        return builder.build();
    }

    private <T> Map<Long, Long> groupBy(
            List<T> rows, Function<T, Long> keyFn, Function<T, Long> valueFn) {
        return rows.stream().collect(Collectors.toMap(keyFn, valueFn));
    }
}
