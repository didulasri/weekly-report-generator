package com.weeklyreportgenerator.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreportgenerator.backend.dto.request.UpdateRoleRequest;
import com.weeklyreportgenerator.backend.dto.request.UpdateUserRequest;
import com.weeklyreportgenerator.backend.dto.response.MessageResponse;
import com.weeklyreportgenerator.backend.dto.response.PagedResponse;
import com.weeklyreportgenerator.backend.dto.response.UserDetailResponse;
import com.weeklyreportgenerator.backend.dto.response.UserResponse;
import com.weeklyreportgenerator.backend.entity.enums.RoleName;
import com.weeklyreportgenerator.backend.service.AdminUserService;
import com.weeklyreportgenerator.backend.service.PasswordResetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Account creation moved to the invitation flow (POST /api/admin/invitations) -- no endpoint here
// lets an admin set or see a password. Password reset only ever triggers an email
// (POST .../send-password-reset) -- an admin never sets or sees the new password either.
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final MessageResponse PASSWORD_RESET_ACK = MessageResponse.builder()
            .message("A password reset link has been sent to this user's email, if their account is active.")
            .build();

    private final AdminUserService adminUserService;
    private final PasswordResetService passwordResetService;

    @GetMapping
    public ResponseEntity<PagedResponse<UserResponse>> listUsers(
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<UserResponse> users = adminUserService.listUsers(role, active, search, pageable);
        return ResponseEntity.ok(PagedResponse.of(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDetailResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserDetail(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(adminUserService.updateUser(id, request));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(adminUserService.changeRole(id, request.getRole()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateUser(@PathVariable Long id) {
        adminUserService.deactivateUser(id);
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.activateUser(id));
    }

    @PostMapping("/{id}/send-password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse sendPasswordReset(@PathVariable Long id) {
        passwordResetService.adminRequestReset(id);
        return PASSWORD_RESET_ACK;
    }
}
