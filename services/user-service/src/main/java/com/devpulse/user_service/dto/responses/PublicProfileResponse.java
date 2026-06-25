package com.devpulse.user_service.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileResponse {
    private UUID id;
    private String username;
    private String displayName;
    private String bio;
    private String avatarUrl;
}
