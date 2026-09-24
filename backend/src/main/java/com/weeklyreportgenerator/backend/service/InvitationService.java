package com.weeklyreportgenerator.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreportgenerator.backend.dto.response.InvitationResponse;
import com.weeklyreportgenerator.backend.dto.response.InvitationValidationResponse;
import com.weeklyreportgenerator.backend.dto.response.UserSummaryResponse;
import com.weeklyreportgenerator.backend.entity.Invitation;
import com.weeklyreportgenerator.backend.entity.Role;
import com.weeklyreportgenerator.backend.entity.User;
import com.weeklyreportgenerator.backend.entity.enums.InvitationStatus;
import com.weeklyreportgenerator.backend.entity.enums.RoleName;
import com.weeklyreportgenerator.backend.event.InvitationCreatedEvent;
import com.weeklyreportgenerator.backend.exception.DuplicateResourceException;
import com.weeklyreportgenerator.backend.exception.InvalidStatusTransitionException;
import com.weeklyreportgenerator.backend.exception.InvitationInvalidException;
import com.weeklyreportgenerator.backend.exception.ResourceNotFoundException;
import com.weeklyreportgenerator.backend.repository.InvitationRepository;
import com.weeklyreportgenerator.backend.repository.RoleRepository;
import com.weeklyreportgenerator.backend.repository.UserRepository;
import com.weeklyreportgenerator.backend.security.SecureTokenService;
import com.weeklyreportgenerator.backend.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final long EXPIRY_HOURS = 48;

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenService secureTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public InvitationResponse createInvitation(String email, RoleName roleName) {
        if (userRepository.existsByEmailIgnoreCaseAndActiveTrue(email)) {
            throw new DuplicateResourceException("An active account with this email already exists");
        }
        if (invitationRepository.existsByEmailIgnoreCaseAndStatus(email, InvitationStatus.PENDING)) {
            throw new DuplicateResourceException(
                    "A pending invitation for this email already exists -- use resend instead of creating a new one");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " role is not seeded"));
        User invitedBy = userRepository.findByIdWithRole(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Inviting admin not found"));

        String rawToken = secureTokenService.generate();
        Instant expiresAt = Instant.now().plus(EXPIRY_HOURS, ChronoUnit.HOURS);

        Invitation invitation = Invitation.builder()
                .email(email)
                .role(role)
                .tokenHash(secureTokenService.hash(rawToken))
                .status(InvitationStatus.PENDING)
                .expiresAt(expiresAt)
                .invitedBy(invitedBy)
                .build();
        invitation = invitationRepository.save(invitation);

        eventPublisher.publishEvent(new InvitationCreatedEvent(
                email, roleName.name(), invitedBy.getName(), rawToken, expiresAt));

        return toResponse(invitation);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Page<InvitationResponse> listInvitations(InvitationStatus status, Pageable pageable) {
        invitationRepository.expireStalePending(Instant.now());
        Page<Invitation> invitations = status != null
                ? invitationRepository.findByStatus(status, pageable)
                : invitationRepository.findAll(pageable);
        return invitations.map(this::toResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public InvitationResponse resendInvitation(Long id) {
        invitationRepository.expireStalePending(Instant.now());
        Invitation invitation = getInvitationOrThrow(id);

        if (invitation.getStatus() != InvitationStatus.PENDING && invitation.getStatus() != InvitationStatus.EXPIRED) {
            throw new InvalidStatusTransitionException(
                    "Only a pending or expired invitation can be resent (current status: " + invitation.getStatus() + ")");
        }

        String rawToken = secureTokenService.generate();
        Instant expiresAt = Instant.now().plus(EXPIRY_HOURS, ChronoUnit.HOURS);

        // Rotating the hash in place invalidates the old link immediately -- a lookup by its hash
        // no longer matches any row.
        invitation.setTokenHash(secureTokenService.hash(rawToken));
        invitation.setExpiresAt(expiresAt);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation = invitationRepository.save(invitation);

        eventPublisher.publishEvent(new InvitationCreatedEvent(
                invitation.getEmail(), invitation.getRole().getName().name(),
                invitation.getInvitedBy().getName(), rawToken, expiresAt));

        return toResponse(invitation);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void revokeInvitation(Long id) {
        Invitation invitation = getInvitationOrThrow(id);
        invitation.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public InvitationValidationResponse validateToken(String rawToken) {
        Invitation invitation = invitationRepository.findByTokenHash(secureTokenService.hash(rawToken))
                .filter(this::isUsable)
                .orElseThrow(InvitationInvalidException::new);

        return InvitationValidationResponse.builder()
                .email(invitation.getEmail())
                .role(invitation.getRole().getName())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }

    // Returns the DTO, not the User entity -- the entity's role association is lazy, and by the
    // time a controller could touch it the transaction (and its persistence context) is closed.
    @Transactional
    public UserSummaryResponse acceptInvitation(String rawToken, String name, String password) {
        Invitation invitation = invitationRepository.findByTokenHashForUpdate(secureTokenService.hash(rawToken))
                .filter(this::isUsable)
                .orElseThrow(InvitationInvalidException::new);

        RoleName roleName = invitation.getRole().getName();

        User user = User.builder()
                .name(name)
                .email(invitation.getEmail())
                .password(passwordEncoder.encode(password))
                .role(invitation.getRole())
                .active(true)
                .build();
        user = userRepository.save(user);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);

        return UserSummaryResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(roleName.name())
                .build();
    }

    // Runs regardless of whether anyone reads the invitations list, so an invitation nobody ever
    // checks on still ends up correctly marked EXPIRED rather than PENDING forever.
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void expireStaleInvitationsDaily() {
        invitationRepository.expireStalePending(Instant.now());
    }

    private boolean isUsable(Invitation invitation) {
        return invitation.getStatus() == InvitationStatus.PENDING
                && invitation.getExpiresAt().isAfter(Instant.now());
    }

    private Invitation getInvitationOrThrow(Long id) {
        return invitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found: " + id));
    }

    private InvitationResponse toResponse(Invitation invitation) {
        return InvitationResponse.builder()
                .id(invitation.getId())
                .email(invitation.getEmail())
                .role(invitation.getRole().getName())
                .status(invitation.getStatus())
                .expiresAt(invitation.getExpiresAt())
                .invitedByName(invitation.getInvitedBy().getName())
                .createdAt(invitation.getCreatedAt())
                .acceptedAt(invitation.getAcceptedAt())
                .build();
    }
}
