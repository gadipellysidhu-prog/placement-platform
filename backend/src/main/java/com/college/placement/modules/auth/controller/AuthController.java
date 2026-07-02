package com.college.placement.modules.auth.controller;

import com.college.placement.modules.auth.dto.ConfirmVerificationRequest;
import com.college.placement.modules.auth.dto.EmailVerificationRequest;
import com.college.placement.modules.auth.dto.ForgotPasswordRequest;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.MessageResponse;
import com.college.placement.modules.auth.dto.RefreshRequest;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.dto.ResetPasswordRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.service.AccountVerificationService;
import com.college.placement.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String GENERIC_EMAIL_MESSAGE =
            "If an account exists for that email, a message has been sent.";

    private final AuthService authService;
    private final AccountVerificationService verificationService;

    public AuthController(AuthService authService,
                          AccountVerificationService verificationService) {
        this.authService = authService;
        this.verificationService = verificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        // Registration commits before this returns; send the verification email outside
        // that transaction so a delivery failure cannot roll back the new account.
        verificationService.requestEmailVerification(request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse(
                "Registration successful. Please check your email to verify your account before signing in."));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email/request")
    public ResponseEntity<MessageResponse> requestEmailVerification(
            @Valid @RequestBody EmailVerificationRequest request) {
        verificationService.requestEmailVerification(request.email());
        return ResponseEntity.accepted().body(new MessageResponse(GENERIC_EMAIL_MESSAGE));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody EmailVerificationRequest request) {
        verificationService.requestEmailVerification(request.email());
        return ResponseEntity.accepted().body(new MessageResponse(GENERIC_EMAIL_MESSAGE));
    }

    @PostMapping("/verify-email/confirm")
    public ResponseEntity<MessageResponse> confirmEmailVerification(
            @Valid @RequestBody ConfirmVerificationRequest request) {
        verificationService.confirmEmailVerification(request.token());
        return ResponseEntity.ok(new MessageResponse("Email verified successfully."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        verificationService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().body(new MessageResponse(GENERIC_EMAIL_MESSAGE));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        verificationService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(new MessageResponse("Password has been reset. Please sign in again."));
    }
}
