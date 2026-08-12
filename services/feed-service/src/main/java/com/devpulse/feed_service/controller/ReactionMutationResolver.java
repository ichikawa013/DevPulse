package com.devpulse.feed_service.controller;

import com.devpulse.feed_service.dto.requests.CreateReactionInput;
import com.devpulse.feed_service.entities.Reaction;
import com.devpulse.feed_service.entities.ReactionType;
import com.devpulse.feed_service.services.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@RequiredArgsConstructor
@Controller
public class ReactionMutationResolver {

    private final ReactionService reactionService;

    @MutationMapping
    public Reaction reactPost(@Argument UUID postId,
                              @Argument ReactionType reactionType,
                              @ContextValue("X-User-Id") String actorEmail) {
        return reactionService.react(new CreateReactionInput(postId, actorEmail, reactionType));
    }
}
