package com.devpulse.feed_service.dto.events;

import java.time.Instant;
import java.util.UUID;

public record PostEvent(
        UUID postId,
        String authorEmail,
        String content,
        Instant createdAt
) {}
