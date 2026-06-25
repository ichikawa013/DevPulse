package com.devpulse.user_service.controller;

import com.devpulse.user_service.dto.requests.LoginRequest;
import com.devpulse.user_service.dto.requests.RegisterRequest;
import com.devpulse.user_service.dto.responses.AuthResponse;
import com.devpulse.user_service.entities.User;
import com.devpulse.user_service.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AuthenticationController {

    @Autowired
    private AuthService authService;

    @MutationMapping
    public AuthResponse register(@Valid @Argument RegisterRequest input) {
        return authService.register(input);
    }

    @MutationMapping
    public AuthResponse login(@Argument LoginRequest input) {
        return authService.login(input);
    }

    @MutationMapping
    public AuthResponse refreshToken(@Argument String refreshToken) {
        return authService.refresh(refreshToken);
    }

    @PreAuthorize("isAuthenticated()")
    @MutationMapping
    public Boolean logout(@AuthenticationPrincipal User user) {
        authService.logout(user.getUsername());
        return true;
    }
}

