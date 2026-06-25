package com.devpulse.user_service.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class UpdateProfileRequest {
    private String displayName;
    private String avatarUrl;
    private String bio;
    @NotBlank(message = "Email can't be empty")
    private String email; //only to look for user
}
