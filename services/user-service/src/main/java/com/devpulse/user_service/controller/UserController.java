package com.devpulse.user_service.controller;

import com.devpulse.user_service.dto.requests.UpdatePasswordRequest;
import com.devpulse.user_service.dto.requests.UpdateProfileRequest;
import com.devpulse.user_service.dto.responses.PersonalProfileResponse;
import com.devpulse.user_service.dto.responses.PublicProfileResponse;
import com.devpulse.user_service.entities.User;
import com.devpulse.user_service.services.UserServices;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class UserController {

    private UserServices userServices;

    @PreAuthorize("isAuthenticated()")
    @QueryMapping
    public PersonalProfileResponse getPersonalProfileResponse(@AuthenticationPrincipal User user) {
        return userServices.getPersonalProfileDetails(user.getEmail());
    }

    @QueryMapping
    public PublicProfileResponse getPublicProfileResponse(@Argument String username) {
        return userServices.getPublicProfileDetails(username);
    }

    @PreAuthorize("isAuthenticated()")
    @MutationMapping
    public PersonalProfileResponse updateProfile(
            @Argument UpdateProfileRequest input,
            @AuthenticationPrincipal User user) {
        return userServices.updateProfile(input, user);
    }

    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    @MutationMapping
    public String changePassword(
            @Argument UpdatePasswordRequest input,
            @AuthenticationPrincipal User user) {

        return userServices.updatePassword(input, user);
    }
}
