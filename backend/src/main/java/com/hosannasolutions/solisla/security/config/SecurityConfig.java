package com.hosannasolutions.solisla.security.config;

import com.hosannasolutions.solisla.security.userdetails.SolIslaUserDetailsService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;

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
            "/api/orders/track/**", "/api/delivery/zones",
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

    /**
     * {@code RateLimitFilter} is a {@code @Component} (so its limits can be {@code @Value}-
     * configured) — Spring Boot's servlet-filter auto-configuration would otherwise register it
     * a *second* time as a generic servlet filter in addition to the explicit
     * {@code addFilterBefore} wiring below, running every request through it twice. Disable that
     * automatic registration; Spring Security's own chain is the only place it should run.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> disableRateLimitFilterAutoRegistration(RateLimitFilter rateLimitFilter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(rateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            SolIslaUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, SecurityContextRepository securityContextRepository, RateLimitFilter rateLimitFilter)
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
                .addFilterBefore(rateLimitFilter, CsrfFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(securityContext -> securityContext.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_API_MATCHERS).permitAll()
                        // Unauthenticated health check only — everything else under /actuator/**
                        // (info, and anything added later) needs a logged-in session, and IIS
                        // never proxies /actuator/** externally anyway (frontend-web.config only
                        // rewrites ^api/(.*)).
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .headers(headers -> headers
                        // Not a Spring Security default — explicit rather than left to chance.
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // Same-origin SPA + API (no CDN scripts) other than the Google Fonts
                        // origins already loaded in frontend/src/index.html. font-src needs
                        // 'self' too, not just the Google Fonts origin — the admin UI's own
                        // PrimeIcons webfont is bundled and served same-origin.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "style-src 'self' https://fonts.googleapis.com 'unsafe-inline'; "
                                        + "font-src 'self' https://fonts.gstatic.com; "
                                        + "img-src 'self' data:"))
                        // Explicit rather than relying on the Spring Security default matching
                        // what's wanted — request.isSecure() already resolves correctly behind
                        // IIS via server.forward-headers-strategy: framework.
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(Duration.ofDays(365).toSeconds())))
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
