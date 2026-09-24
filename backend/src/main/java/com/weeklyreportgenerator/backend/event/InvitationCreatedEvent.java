package com.weeklyreportgenerator.backend.event;

import java.time.Instant;

// Carries only primitive/value data, not the Invitation entity -- the listener runs AFTER_COMMIT
// in a separate thread (@Async), by which point a request-scoped persistence context (and any
// lazy association on the entity) is long gone.
public record InvitationCreatedEvent(
        String recipientEmail,
        String recipientRole,
        String invitedByName,
        String rawToken,
        Instant expiresAt) {
}
