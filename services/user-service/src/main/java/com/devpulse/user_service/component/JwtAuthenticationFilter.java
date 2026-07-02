package com.devpulse.user_service.component;

import com.devpulse.user_service.services.CustomUserDetailsService;
import com.devpulse.user_service.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
            @lombok.NonNull HttpServletRequest request,
            @lombok.NonNull HttpServletResponse response,
            @lombok.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {

            log.info("========== JWT FILTER ==========");
            log.info("Request URI: {}", request.getRequestURI());

            final String authHeader = request.getHeader("Authorization");
            log.info("Authorization Header: {}", authHeader);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("No Bearer token found.");
                filterChain.doFilter(request, response);
                return;
            }

            final String token = authHeader.substring(7);
            log.info("JWT Token: {}", token);

            final String username = jwtService.extractUsername(token);
            log.info("Username extracted from token: {}", username);

            log.info("Existing Authentication: {}",
                    SecurityContextHolder.getContext().getAuthentication());

            if (username != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails =
                        customUserDetailsService.loadUserByUsername(username);

                log.info("Loaded User: {}", userDetails.getUsername());
                log.info("Authorities: {}", userDetails.getAuthorities());

                boolean isValid = jwtService.isTokenValid(token, userDetails);
                log.info("Is Token Valid: {}", isValid);

                if (isValid) {

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authenticationToken);

                    log.info("Authentication successfully set.");
                    log.info("Current Authentication: {}",
                            SecurityContextHolder.getContext().getAuthentication());
                    log.info("Authenticated Authorities: {}",
                            SecurityContextHolder.getContext()
                                    .getAuthentication()
                                    .getAuthorities());

                } else {
                    log.warn("Token validation failed.");
                }
            }

        } catch (Exception e) {
            log.error("JWT Filter Exception", e);
        }

        filterChain.doFilter(request, response);
    }
}