package com.akbyk.cryptopal.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    public SessionAuthenticationFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or malformed Authorization header");
            return;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        String userId = redisTemplate.opsForValue().get("session:" + token);

        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired or invalid token");
            return;
        }

        AuthenticatedUserContext.setCurrentUserId(UUID.fromString(userId));
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuthenticatedUserContext.clear();
        }
    }
}