package com.devpulse.user_service.services;

import com.devpulse.user_service.dto.requests.UpdatePasswordRequest;
import com.devpulse.user_service.dto.requests.UpdateProfileRequest;
import com.devpulse.user_service.dto.responses.PersonalProfileResponse;
import com.devpulse.user_service.dto.responses.PublicProfileResponse;
import com.devpulse.user_service.entities.User;
import com.devpulse.user_service.exception.BadRequestHandler;
import com.devpulse.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServices {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    public PersonalProfileResponse getPersonalProfileDetails(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestHandler("User not found"));

        return new PersonalProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl()
        );
    }

    public PublicProfileResponse getPublicProfileDetails(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new PublicProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl()
        );
    }

    public PersonalProfileResponse updateProfile(UpdateProfileRequest input, User user) {
        if (input.getDisplayName() != null) user.setDisplayName(input.getDisplayName());
        if (input.getBio() != null) user.setBio(input.getBio());
        if (input.getAvatarUrl() != null) user.setAvatarUrl(input.getAvatarUrl());

        userRepository.save(user);

        return new PersonalProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl()
        );
    }

    public String updatePassword(UpdatePasswordRequest input, User user) {
        if (!passwordEncoder.matches(input.getOldPassword(), user.getPassword())) {
            throw new BadRequestHandler("Current password is incorrect");
        }

        if (passwordEncoder.matches(input.getNewPassword(), user.getPassword())) {
            throw new BadRequestHandler("New password must be different");
        }

        user.setPassword(passwordEncoder.encode(input.getNewPassword()));

        userRepository.save(user);

        return "Password Updated";
    }
}
