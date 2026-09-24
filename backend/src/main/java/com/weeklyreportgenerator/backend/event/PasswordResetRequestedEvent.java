package com.weeklyreportgenerator.backend.event;

import java.time.Instant;

// Carries only primitive/value data, not the User/PasswordResetToken entities -- see
// InvitationCreatedEvent for why (AFTER_COMMIT + @Async means no live persistence context).
public record PasswordResetRequestedEvent(
        String recipientEmail,
        String recipientName,
        String rawToken,
        Instant expiresAt) {
}
