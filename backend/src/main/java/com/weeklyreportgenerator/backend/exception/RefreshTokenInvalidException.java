package com.weeklyreportgenerator.backend.exception;

// Covers every reason a presented refresh token doesn't work: unknown, expired, or already
// revoked (including the reuse-detected case, where the whole family was just revoked because of
// this same presentation). Always maps to 401 with cookies cleared.
public class RefreshTokenInvalidException extends RuntimeException {

    public RefreshTokenInvalidException(String message) {
        super(message);
    }
}
