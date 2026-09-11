package com.hosannasolutions.solisla.security.config;

import com.hosannasolutions.solisla.security.userdetails.SolIslaUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Server-side session + httpOnly cookie + CSRF, not JWT — same reasoning as SweetHome: this
 * project's deployment is same-origin via IIS/ARR, admin-only login (customers use guest
 * checkout, never authenticate), so a JWT scheme buys nothing. No CORS configuration: same-origin
 * means it's unnecessary and would only be a foot-gun.
 * <p>
 * Guest checkout is the MVP default (CLAUDE.md rule 11): the public storefront surface
 * (catalog/category browsing, cart, checkout, order tracking) is open to anyone, while
 * everything under {@code /api/admin/**} requires authentication plus the endpoint's own
 * {@code @PreAuthorize} permission check (CLAUDE.md rule 9 — Angular route guards are UX only).
 * Extend {@link #PUBLIC_API_MATCHERS} as those controllers are built, rather than opening
 * {@code /api/**} broadly.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Guest-accessible storefront API surface — no admin data, no customer accounts. */
    private static final String[] PUBLIC_API_MATCHERS = {
            "/api/auth/login", "/api/auth/me",
            "/api/products/**", "/api/categories/**",
            "/api/cart/**", "/api/checkout/**",
            "/api/orders/track/**",
            "/media/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            SolIslaUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository)
            throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // Angular's XSRF interceptor reads the raw cookie value and echoes it back
                        // verbatim as a header — the default XorCsrfTokenRequestAttributeHandler
                        // expects a masked value instead and would reject that. The plain handler
                        // matches this cookie/header "double submit" pattern (documented Spring
                        // Security recipe for SPA clients).
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(securityContext -> securityContext.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_API_MATCHERS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorizedEntryPoint()))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable());
        return http.build();
    }

    /**
     * Without an explicit entry point, disabling both httpBasic and formLogin leaves Spring
     * Security's default {@code Http403ForbiddenEntryPoint} in place — an unauthenticated
     * request would get 403, not 401. A REST API should distinguish "not authenticated" (401)
     * from "authenticated but not allowed" (403, from {@code @PreAuthorize}).
     */
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
