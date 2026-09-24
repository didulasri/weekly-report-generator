package com.weeklyreportgenerator.backend.exception;

// Thrown for every failure reason on an invitation token -- expired, revoked, already accepted,
// or simply unknown. Always maps to the same 410 response so a caller can never distinguish "this
// token never existed" from "this token was already used", which would otherwise leak information.
public class InvitationInvalidException extends RuntimeException {

    private static final String MESSAGE = "This invitation link is invalid or has expired";

    public InvitationInvalidException() {
        super(MESSAGE);
    }
}
