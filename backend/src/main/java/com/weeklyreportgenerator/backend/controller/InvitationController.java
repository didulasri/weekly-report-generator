package com.weeklyreportgenerator.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreportgenerator.backend.dto.request.AcceptInvitationRequest;
import com.weeklyreportgenerator.backend.dto.request.ValidateInvitationRequest;
import com.weeklyreportgenerator.backend.dto.response.InvitationValidationResponse;
import com.weeklyreportgenerator.backend.dto.response.UserSummaryResponse;
import com.weeklyreportgenerator.backend.service.InvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Anonymous-accessible (permitAll in SecurityConfig) -- these are pre-authentication endpoints: a
// visitor with an invitation link has no account and therefore no way to authenticate yet.
// POST + JSON body throughout, not GET + query string, so the token never lands in server access
// logs.
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/validate")
    public ResponseEntity<InvitationValidationResponse> validate(@Valid @RequestBody ValidateInvitationRequest request) {
        return ResponseEntity.ok(invitationService.validateToken(request.getToken()));
    }

    @PostMapping("/accept")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummaryResponse accept(@Valid @RequestBody AcceptInvitationRequest request) {
        return invitationService.acceptInvitation(request.getToken(), request.getName(), request.getPassword());
    }
}
