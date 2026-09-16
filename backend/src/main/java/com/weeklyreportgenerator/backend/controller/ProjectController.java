package com.weeklyreportgenerator.backend.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreportgenerator.backend.dto.request.AssignMemberRequest;
import com.weeklyreportgenerator.backend.dto.request.ProjectRequest;
import com.weeklyreportgenerator.backend.dto.response.PagedResponse;
import com.weeklyreportgenerator.backend.dto.response.ProjectMemberResponse;
import com.weeklyreportgenerator.backend.dto.response.ProjectResponse;
import com.weeklyreportgenerator.backend.entity.Project;
import com.weeklyreportgenerator.backend.entity.UserProject;
import com.weeklyreportgenerator.backend.entity.enums.ProjectStatus;
import com.weeklyreportgenerator.backend.service.ProjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<PagedResponse<ProjectResponse>> listProjects(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Project> projects = projectService.listProjects(status, search, pageable);
        return ResponseEntity.ok(PagedResponse.of(projects.map(this::toResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(projectService.getProject(id)));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        Project created = projectService.createProject(request.getName(), request.getDescription(), request.getStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        Project updated = projectService.updateProject(id, request.getName(), request.getDescription(), request.getStatus());
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateProject(@PathVariable Long id) {
        projectService.deactivateProject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<ProjectMemberResponse>> listMembers(@PathVariable Long id) {
        List<ProjectMemberResponse> members = projectService.listMembers(id).stream()
                .map(this::toMemberResponse)
                .toList();
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ProjectMemberResponse> assignMember(
            @PathVariable Long id, @Valid @RequestBody AssignMemberRequest request) {
        UserProject assigned = projectService.assignMember(id, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toMemberResponse(assigned));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        projectService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus().name())
                .memberCount(projectService.countActiveMembers(project.getId()))
                .createdAt(project.getCreatedAt())
                .build();
    }

    private ProjectMemberResponse toMemberResponse(UserProject userProject) {
        return ProjectMemberResponse.builder()
                .userId(userProject.getUser().getId())
                .name(userProject.getUser().getName())
                .email(userProject.getUser().getEmail())
                .role(userProject.getUser().getRole().getName().name())
                .assignedAt(userProject.getAssignedAt())
                .build();
    }
}
