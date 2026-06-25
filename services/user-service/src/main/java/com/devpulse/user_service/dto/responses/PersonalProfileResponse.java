package com.devpulse.user_service.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonalProfileResponse {
    private UUID id;
    private String username;
    private String email;
    private String displayName;
    private String bio;
    private String avatarUrl;
}
