package com.hosannasolutions.solisla.security.dto;

import com.hosannasolutions.solisla.security.authorization.Role;
import com.hosannasolutions.solisla.security.userdetails.SolIslaUserPrincipal;
import java.util.UUID;

public record AuthenticatedUserResponse(
        UUID id,
        String email,
        Role role
) {

    public static AuthenticatedUserResponse from(SolIslaUserPrincipal principal) {
        return new AuthenticatedUserResponse(principal.getUserId(), principal.getUsername(), principal.getRole());
    }
}
