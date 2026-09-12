package com.hosannasolutions.solisla.security.config;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Best-effort brute-force/abuse guard for the three endpoints where it actually matters:
 * credential stuffing on login, order spam on checkout, and — most importantly — enumeration
 * attempts against {@code /api/orders/track} (design doc §53's whole point was that order number
 * + phone can't be used to enumerate orders one request at a time; that protection is worthless
 * without also limiting how many requests an attacker gets).
 * <p>
 * A per-client-IP sliding window kept in memory ({@code ConcurrentHashMap}, no new dependency) —
 * correct for this app's single Windows Service instance (CLAUDE.md rule 12: no Redis/shared
 * store until actually horizontally scaled, which isn't the plan here). {@code request.getRemoteAddr()}
 * already resolves to the real client IP behind IIS because {@code server.forward-headers-strategy:
 * framework} enables Spring's {@code ForwardedHeaderFilter}, which runs ahead of this filter.
 * <p>
 * A {@code @Component} rather than a plain {@code new RateLimitFilter()} so limits are
 * {@code @Value}-configurable (tests override them to generous values via
 * {@code src/test/resources/application.yml} — production's brute-force tuning shouldn't dictate
 * how many logins a test fixture is allowed to perform) and so an {@code ObjectMapper} can be
 * injected for the 429 body instead of hand-built JSON string concatenation.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Limit(int maxRequests, Duration window) {
    }

    private final Map<String, Limit> limitedPaths;
    private final Duration maxWindow;
    private final ObjectMapper objectMapper;
    private final Map<String, Deque<Instant>> requestTimestampsByKey = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;

    public RateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${sol-isla.rate-limit.login.max-requests:5}") int loginMaxRequests,
            @Value("${sol-isla.rate-limit.login.window-minutes:5}") long loginWindowMinutes,
            @Value("${sol-isla.rate-limit.checkout.max-requests:10}") int checkoutMaxRequests,
            @Value("${sol-isla.rate-limit.checkout.window-minutes:1}") long checkoutWindowMinutes,
            @Value("${sol-isla.rate-limit.track.max-requests:10}") int trackMaxRequests,
            @Value("${sol-isla.rate-limit.track.window-minutes:1}") long trackWindowMinutes) {
        this.objectMapper = objectMapper;
        this.limitedPaths = Map.of(
                "/api/auth/login", new Limit(loginMaxRequests, Duration.ofMinutes(loginWindowMinutes)),
                "/api/checkout", new Limit(checkoutMaxRequests, Duration.ofMinutes(checkoutWindowMinutes)),
                "/api/orders/track", new Limit(trackMaxRequests, Duration.ofMinutes(trackWindowMinutes))
        );
        this.maxWindow = limitedPaths.values().stream().map(Limit::window).max(Duration::compareTo).orElse(Duration.ZERO);

        // Without this, an IP that hits a limited path exactly once (or an attacker rotating
        // source IPs) leaves a permanent entry in requestTimestampsByKey — a slow, unbounded
        // memory leak for the lifetime of the process, on exactly the code path meant to resist
        // abuse. Daemon thread: never blocks JVM/context shutdown.
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "rate-limit-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        this.cleanupExecutor.scheduleAtFixedRate(this::evictStaleEntries, 10, 10, TimeUnit.MINUTES);
    }

    @PreDestroy
    void shutdown() {
        cleanupExecutor.shutdownNow();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Limit limit = limitedPaths.get(request.getRequestURI());
        if (limit == null || isAllowed(request.getRemoteAddr() + ":" + request.getRequestURI(), limit)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(limit.window().toSeconds()));
        // response.getWriter() defaults to ISO-8859-1 unless told otherwise, which mangles
        // anything outside that range (Jackson itself writes correct UTF-8 bytes regardless).
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "Too many requests, please try again later");
        objectMapper.writeValue(response.getWriter(), body);
    }

    private boolean isAllowed(String key, Limit limit) {
        Deque<Instant> timestamps = requestTimestampsByKey.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        Instant now = Instant.now();
        Instant windowStart = now.minus(limit.window());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limit.maxRequests()) {
                return false;
            }
            timestamps.addLast(now);
        }
        return true;
    }

    private void evictStaleEntries() {
        Instant cutoff = Instant.now().minus(maxWindow);
        requestTimestampsByKey.entrySet().removeIf(entry -> {
            Deque<Instant> timestamps = entry.getValue();
            synchronized (timestamps) {
                return timestamps.isEmpty() || timestamps.peekLast().isBefore(cutoff);
            }
        });
    }
}
