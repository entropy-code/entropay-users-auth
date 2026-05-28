package com.entropyteam.entropay.users.auth.config;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Coarse per-IP sliding-window rate limiter for the public Dynamic Client Registration
 * endpoint, which creates AWS Cognito app clients. Defence-in-depth on top of the
 * redirect_uri allowlist. Registered only for /oauth2/register (see McpAuthGatewayConfig).
 */
public class DcrRateLimitFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DcrRateLimitFilter.class);
    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MILLIS = 60 * 60 * 1000L;

    private final Map<String, Deque<Long>> hitsByClient = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        if (isRateLimited(clientIp)) {
            LOGGER.warn("Rate limit exceeded for client registration from {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"temporarily_unavailable\","
                            + "\"error_description\":\"registration rate limit exceeded\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String clientIp) {
        long now = System.currentTimeMillis();
        Deque<Long> hits = hitsByClient.computeIfAbsent(clientIp, key -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && now - hits.peekFirst() > WINDOW_MILLIS) {
                hits.pollFirst();
            }
            if (hits.size() >= MAX_REQUESTS) {
                return true;
            }
            hits.addLast(now);
            return false;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
