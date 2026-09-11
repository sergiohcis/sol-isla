package com.hosannasolutions.solisla.security.controller;

import com.hosannasolutions.solisla.security.dto.AuthenticatedUserResponse;
import com.hosannasolutions.solisla.security.dto.LoginRequest;
import com.hosannasolutions.solisla.security.providedService.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthenticatedUserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {
        return authService.login(request, httpRequest, httpResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(httpRequest, httpResponse);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me() {
        AuthenticatedUserResponse response = authService.currentUser();
        return response == null ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build() : ResponseEntity.ok(response);
    }

    /**
     * Generic message on the wire for both wrong-password and disabled-account cases, to avoid
     * account-status enumeration — the real reason is captured in {@code audit_events} instead
     * (see {@code AuthServiceImpl.recordLoginFailure}).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
}
