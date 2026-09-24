package com.weeklyreportgenerator.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

import com.weeklyreportgenerator.backend.event.InvitationCreatedEvent;
import com.weeklyreportgenerator.backend.event.PasswordResetRequestedEvent;

import lombok.RequiredArgsConstructor;

// AFTER_COMMIT so an email is never sent for a transaction that ends up rolling back (e.g. the
// invitation row itself failed to save), and @Async so a slow SMTP server never adds latency to
// the HTTP response -- by the time these run, the response has typically already been sent.
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvitationCreated(InvitationCreatedEvent event) {
        String link = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/accept-invitation")
                .queryParam("token", event.rawToken())
                .toUriString();
        emailService.sendInvitationEmail(
                event.recipientEmail(), event.recipientRole(), event.invitedByName(), link, event.expiresAt());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        String link = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/reset-password")
                .queryParam("token", event.rawToken())
                .toUriString();
        emailService.sendPasswordResetEmail(event.recipientEmail(), event.recipientName(), link, event.expiresAt());
    }
}
