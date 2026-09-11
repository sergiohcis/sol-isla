package com.hosannasolutions.solisla.security.providedService;

import com.hosannasolutions.solisla.audit.AuditAction;
import com.hosannasolutions.solisla.audit.dto.AuditEventRequest;
import com.hosannasolutions.solisla.audit.providedService.AuditService;
import com.hosannasolutions.solisla.security.dto.AuthenticatedUserResponse;
import com.hosannasolutions.solisla.security.dto.LoginRequest;
import com.hosannasolutions.solisla.security.userdetails.SolIslaUserPrincipal;
import com.hosannasolutions.solisla.user.User;
import com.hosannasolutions.solisla.user.providedService.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AuditService auditService;
    private final UserService userService;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            AuditService auditService,
            UserService userService
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.auditService = auditService;
        this.userService = userService;
    }

    @Override
    public AuthenticatedUserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            Authentication authenticated = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticated);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            SolIslaUserPrincipal principal = (SolIslaUserPrincipal) authenticated.getPrincipal();
            auditService.record(AuditEventRequest.of(AuditAction.USER_LOGIN_SUCCEEDED, principal.getUserId()));
            return AuthenticatedUserResponse.from(principal);
        } catch (AuthenticationException ex) {
            recordLoginFailure(request.email(), ex);
            throw ex;
        }
    }

    @Override
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        UUID userId = currentPrincipal().map(SolIslaUserPrincipal::getUserId).orElse(null);
        var session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        auditService.record(AuditEventRequest.of(AuditAction.USER_LOGOUT, userId));
    }

    @Override
    public AuthenticatedUserResponse currentUser() {
        return currentPrincipal().map(AuthenticatedUserResponse::from).orElse(null);
    }

    private void recordLoginFailure(String email, AuthenticationException ex) {
        UUID userId = userService.findByEmail(email).map(User::getId).orElse(null);
        String reason = ex instanceof DisabledException ? "DISABLED" : "BAD_CREDENTIALS";
        auditService.record(AuditEventRequest.of(AuditAction.USER_LOGIN_FAILED, userId, Map.of("reason", reason)));
    }

    private Optional<SolIslaUserPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SolIslaUserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }
}
