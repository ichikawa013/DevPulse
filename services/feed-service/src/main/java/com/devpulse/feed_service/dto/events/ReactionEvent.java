package com.devpulse.feed_service.dto.events;

import java.time.Instant;
import java.util.UUID;

public record ReactionEvent(
        UUID postId,
        String actorEmail,
        String postAuthorEmail,
        String reactionType,
        Instant createdAt
)
{}
