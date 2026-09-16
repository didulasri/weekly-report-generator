package com.weeklyreportgenerator.backend.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreportgenerator.backend.entity.Project;
import com.weeklyreportgenerator.backend.entity.User;
import com.weeklyreportgenerator.backend.entity.UserProject;
import com.weeklyreportgenerator.backend.entity.enums.ProjectStatus;
import com.weeklyreportgenerator.backend.exception.DuplicateResourceException;
import com.weeklyreportgenerator.backend.exception.ResourceNotFoundException;
import com.weeklyreportgenerator.backend.repository.ProjectRepository;
import com.weeklyreportgenerator.backend.repository.UserProjectRepository;
import com.weeklyreportgenerator.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserProjectRepository userProjectRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<Project> listProjects(ProjectStatus status, String search, Pageable pageable) {
        return projectRepository.findAll(status, search, pageable);
    }

    @Transactional(readOnly = true)
    public Project getProject(Long id) {
        return getProjectOrThrow(id);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional
    public Project createProject(String name, String description, ProjectStatus status) {
        if (projectRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A project named '" + name + "' already exists");
        }

        Project project = Project.builder()
                .name(name)
                .description(description)
                .status(status != null ? status : ProjectStatus.ACTIVE)
                .build();

        return projectRepository.save(project);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional
    public Project updateProject(Long id, String name, String description, ProjectStatus status) {
        Project project = getProjectOrThrow(id);

        if (!project.getName().equalsIgnoreCase(name) && projectRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A project named '" + name + "' already exists");
        }

        project.setName(name);
        project.setDescription(description);
        if (status != null) {
            project.setStatus(status);
        }

        return projectRepository.save(project);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional
    public void deactivateProject(Long id) {
        Project project = getProjectOrThrow(id);
        project.setStatus(ProjectStatus.INACTIVE);
        projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<UserProject> listMembers(Long projectId) {
        getProjectOrThrow(projectId);
        return userProjectRepository.findActiveWithUserByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public long countActiveMembers(Long projectId) {
        return userProjectRepository.countByProjectIdAndActiveTrue(projectId);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional
    public UserProject assignMember(Long projectId, Long userId) {
        Project project = getProjectOrThrow(projectId);

        if (project.getStatus() == ProjectStatus.INACTIVE) {
            throw new DuplicateResourceException("Cannot assign a member to an inactive project");
        }

        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        var existing = userProjectRepository.findByUserIdAndProjectId(userId, projectId);
        if (existing.isPresent()) {
            UserProject userProject = existing.get();
            if (userProject.isActive()) {
                throw new DuplicateResourceException("User is already assigned to this project");
            }
            userProject.setUser(user);
            userProject.setActive(true);
            userProject.setAssignedAt(Instant.now());
            return userProjectRepository.save(userProject);
        }

        UserProject userProject = UserProject.builder()
                .user(user)
                .project(project)
                .assignedAt(Instant.now())
                .active(true)
                .build();

        return userProjectRepository.save(userProject);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional
    public void removeMember(Long projectId, Long userId) {
        getProjectOrThrow(projectId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        UserProject userProject = userProjectRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User " + userId + " is not assigned to project " + projectId));

        userProject.setActive(false);
        userProjectRepository.save(userProject);
    }

    private Project getProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }
}
