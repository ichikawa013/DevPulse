package com.devpulse.feed_service.services;

import com.devpulse.feed_service.dto.events.ReactionEvent;
import com.devpulse.feed_service.dto.requests.CreateReactionInput;
import com.devpulse.feed_service.entities.Post;
import com.devpulse.feed_service.entities.Reaction;
import com.devpulse.feed_service.exception.ResourceNotFoundHandler;
import com.devpulse.feed_service.repository.PostRepository;
import com.devpulse.feed_service.repository.ReactionRepository;
import io.github.robsonkades.uuidv7.UUIDv7;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Slf4j
@Service
public class ReactionService {

    private final PostRepository postRepository;
    private final ReactionRepository reactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Reaction react(CreateReactionInput input) {
        Post post = postRepository.findById(input.getPostId())
                .orElseThrow(() ->new ResourceNotFoundHandler("Post not found" + input.getPostId()));

        Reaction reaction = reactionRepository.findByPostIdAndActorEmail(input.getPostId(), input.getActorEmail())
                .map(existing -> { existing.setReactionType(input.getReactionType()); return existing; })
                .orElseGet(() -> new Reaction(UUIDv7.randomUUID(), input.getPostId(), input.getActorEmail(), input.getReactionType()));

        reactionRepository.save(reaction);

        kafkaTemplate.send("reaction-events", new ReactionEvent(
                input.getPostId(), input.getActorEmail(), post.getAuthorEmail(), input.getReactionType().name(), Instant.now()
        ));

        return reaction;
    }
}
