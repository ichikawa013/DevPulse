package com.devpulse.feed_service.dto.requests;

import com.devpulse.feed_service.entities.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.graphql.data.method.annotation.Argument;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class CreateReactionInput {
    private UUID postId;
    private String actorEmail;
    private ReactionType reactionType;
}
