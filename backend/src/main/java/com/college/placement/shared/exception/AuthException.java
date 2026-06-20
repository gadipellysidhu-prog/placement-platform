package com.college.placement.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AuthException extends ResponseStatusException {

    public AuthException(HttpStatus status, String reason) {
        super(status, reason);
    }

    public static AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }

    public static AuthException invalidRefreshToken() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired.");
    }

    public static AuthException emailAlreadyRegistered() {
        return new AuthException(HttpStatus.CONFLICT, "Email is already registered.");
    }

    public static AuthException accountLocked() {
        return new AuthException(HttpStatus.FORBIDDEN, "Account is locked.");
    }
}
