package com.soa.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Allow-Credentials", "true");

        if (request.getMethod().equals("OPTIONS")) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        try {
            String userId = null;

            // First, try to extract userId from Authorization header (JWT token)
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String[] parts = token.split("\\.");
                
                if (parts.length >= 2) {
                    try {
                        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> payload = mapper.readValue(payloadJson, Map.class);
                        userId = (String) payload.get("userId");
                    } catch (Exception e) {
                        logger.warn("Failed to parse JWT token", e);
                    }
                }
            }

            // Fallback: check for X-User-Id header (set by gateway)
            if (userId == null) {
                userId = request.getHeader("X-User-Id");
            }

            if (userId != null && !userId.isEmpty()) {
                request.setAttribute("userId", userId);
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Cannot set user authentication", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        }
    }

    @Override
protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    return false;
}
}