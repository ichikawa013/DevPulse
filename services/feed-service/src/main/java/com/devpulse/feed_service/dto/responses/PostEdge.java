package com.devpulse.feed_service.dto.responses;

import com.devpulse.feed_service.entities.Post;

public record PostEdge (
        Post node,
        String cursor
){}
