package com.devpulse.feed_service.dto.requests;

import jakarta.validation.constraints.NotBlank;

public record CreatePostInput (
    @NotBlank
    String content,
    String imageUrl
){}
