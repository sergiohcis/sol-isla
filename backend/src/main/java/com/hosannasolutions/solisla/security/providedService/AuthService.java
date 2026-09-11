package com.hosannasolutions.solisla.security.providedService;

import com.hosannasolutions.solisla.security.dto.AuthenticatedUserResponse;
import com.hosannasolutions.solisla.security.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    AuthenticatedUserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    AuthenticatedUserResponse currentUser();
}
