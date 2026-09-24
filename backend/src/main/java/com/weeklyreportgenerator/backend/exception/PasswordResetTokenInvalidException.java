package com.weeklyreportgenerator.backend.exception;

// Thrown for every failure reason on a password reset token -- unknown, expired, or already used.
// Always maps to the same 410 response so a caller can never distinguish the reasons apart.
public class PasswordResetTokenInvalidException extends RuntimeException {

    private static final String MESSAGE = "This password reset link is invalid or has expired";

    public PasswordResetTokenInvalidException() {
        super(MESSAGE);
    }
}
