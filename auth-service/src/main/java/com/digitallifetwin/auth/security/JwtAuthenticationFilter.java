package com.digitallifetwin.auth.security;

import com.digitallifetwin.auth.dto.response.ErrorResponse;
import com.digitallifetwin.auth.enums.AccountStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty() && jwtService.isValid(token)) {
                if (!authenticateFromDatabase(token, request, response)) {
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * @return false when the response has already been written and the chain must stop
     */
    private boolean authenticateFromDatabase(
            String token, HttpServletRequest request, HttpServletResponse response) throws IOException {
        UUID userId = jwtService.extractUserId(token);
        CustomUserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserById(userId);
        } catch (UsernameNotFoundException ex) {
            if (isPublic(request)) {
                return true;
            }
            writeError(response, request, HttpStatus.UNAUTHORIZED, "Authentication required");
            return false;
        }

        if (userDetails.getAccountStatus() != AccountStatus.ACTIVE) {
            if (isPublic(request)) {
                return true;
            }
            String message = userDetails.getAccountStatus() == AccountStatus.BLOCKED
                    ? "Account is blocked"
                    : "Account is disabled";
            writeError(response, request, HttpStatus.FORBIDDEN, message);
            return false;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return true;
    }

    private boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/google")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/api/auth/forgot-password")
                || path.startsWith("/api/auth/reset-password")
                || path.startsWith("/api/auth/contact")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator/health")
                || path.equals("/actuator/info");
    }

    private void writeError(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String message) throws IOException {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.name(),
                message,
                request.getRequestURI(),
                null
        );
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
