package com.hosannasolutions.solisla.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring Security's default {@code XorCsrfTokenRequestAttributeHandler} defers resolving the
 * CSRF token until something reads it — without this filter forcing that read on every request,
 * the {@code XSRF-TOKEN} cookie a single-page app needs to echo back never actually gets set.
 * This is the documented recipe for Angular/SPA CSRF with Spring Security 6+.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
