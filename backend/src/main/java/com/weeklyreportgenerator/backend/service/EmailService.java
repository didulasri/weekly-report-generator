package com.weeklyreportgenerator.backend.service;

import java.time.Instant;

// Kept separate from the event listener that triggers it (EmailEventListener) so tests can supply
// a mock implementation and assert what would have been sent, without a real SMTP server.
public interface EmailService {

    void sendInvitationEmail(
            String toEmail, String recipientRole, String invitedByName, String invitationLink, Instant expiresAt);

    void sendPasswordResetEmail(
            String toEmail, String recipientName, String resetLink, Instant expiresAt);
}
