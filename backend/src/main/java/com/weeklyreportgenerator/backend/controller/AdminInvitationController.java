package com.weeklyreportgenerator.backend.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreportgenerator.backend.dto.request.CreateInvitationRequest;
import com.weeklyreportgenerator.backend.dto.response.InvitationResponse;
import com.weeklyreportgenerator.backend.dto.response.PagedResponse;
import com.weeklyreportgenerator.backend.entity.enums.InvitationStatus;
import com.weeklyreportgenerator.backend.service.InvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/invitations")
@RequiredArgsConstructor
public class AdminInvitationController {

    private final InvitationService invitationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse createInvitation(@Valid @RequestBody CreateInvitationRequest request) {
        return invitationService.createInvitation(request.getEmail(), request.getRole());
    }

    @GetMapping
    public ResponseEntity<PagedResponse<InvitationResponse>> listInvitations(
            @RequestParam(required = false) InvitationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InvitationResponse> invitations = invitationService.listInvitations(status, pageable);
        return ResponseEntity.ok(PagedResponse.of(invitations));
    }

    @PostMapping("/{id}/resend")
    public ResponseEntity<InvitationResponse> resendInvitation(@PathVariable Long id) {
        return ResponseEntity.ok(invitationService.resendInvitation(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvitation(@PathVariable Long id) {
        invitationService.revokeInvitation(id);
    }
}
