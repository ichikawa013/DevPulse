package com.devpulse.feed_service.controller;

import com.devpulse.feed_service.dto.requests.CreatePostInput;
import com.devpulse.feed_service.dto.responses.PostConnection;
import com.devpulse.feed_service.entities.Post;
import com.devpulse.feed_service.services.PostService;
import com.devpulse.feed_service.services.PostSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RequiredArgsConstructor
@Controller
public class PostGraphQLController {

    private final PostService postService;
    private final PostSubscriptionService postSubscriptionService;

    @MutationMapping
    public Post createPost(@ContextValue("X-User-Id") String authorEmail,
                           @Argument @Valid CreatePostInput input) {

        return postService.createPost(authorEmail, input);
    }

    @MutationMapping
    public Boolean deletePost(@Argument UUID id, @ContextValue("X-User-Id") String callerEmail) {


        return postService.deletePost(id, callerEmail);
    }

    // Listener/subscriber side of the Sinks.Many pipeline.
    // PostService.createPost() is the PRODUCER — it calls postSubscriptionService.emit(post)
    // to push a new Post into the sink, but that alone reaches no one.
    // This method is the CONSUMER-facing wire: when a client sends `subscription { postCreated }`,
    // Spring for GraphQL calls this method ONCE per subscribing client, gets back the Flux<Post>,
    // and then keeps that client's WebSocket connection open, pushing every future sink emission
    // to them automatically. The client never calls this method directly or repeatedly.
    @SubscriptionMapping
    public Flux<Post> postCreated() {
        return postSubscriptionService.stream();
    }

    @QueryMapping
    public PostConnection feed(@Argument Integer first, @Argument String after) {
        return postService.getFeed(first, after);
    }
}