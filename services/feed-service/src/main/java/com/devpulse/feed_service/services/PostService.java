package com.devpulse.feed_service.services;

import com.devpulse.feed_service.dto.events.PostEvent;
import com.devpulse.feed_service.dto.requests.CreatePostInput;
import com.devpulse.feed_service.dto.responses.PageInfo;
import com.devpulse.feed_service.dto.responses.PostConnection;
import com.devpulse.feed_service.dto.responses.PostEdge;
import com.devpulse.feed_service.entities.Post;
import com.devpulse.feed_service.repository.PostRepository;
import io.github.robsonkades.uuidv7.UUIDv7;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class PostService {


    private final PostRepository postRepository;
    private final KafkaTemplate<String, PostEvent> kafkaTemplate;

    public Post createPost(String authorEmail, CreatePostInput input) {
        Post post = new Post();
        post.setId(UUIDv7.randomUUID());
        post.setAuthorEmail(authorEmail);
        post.setContent(input.content());
        post.setImageUrl(input.imageUrl());

        postRepository.save(post);
        log.info("createdAt after save = {}", post.getCreatedAt());

        PostEvent event = new PostEvent(post.getId(), post.getAuthorEmail(), post.getContent(), post.getCreatedAt());
        kafkaTemplate.send("post-events", event);

        return post;
    }

    public Boolean deletePost(UUID id, String callerEmail) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Post not found"));

        if (!post.getAuthorEmail().equals(callerEmail)) {
            throw new AccessDeniedException("Not authorized to delete this post");
        }

        postRepository.delete(post);
        return true;
    }

    private String encodeCursor(Instant createdAt, UUID id) {
        String raw = createdAt.toString() + "|" + id.toString();
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private  record DecodedCursor(Instant createdAt, UUID id) {}

    private DecodedCursor decodedCursor(String cursor) {
        String raw = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
        String[] parts = raw.split("\\|", 2);
        return new DecodedCursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
    }

    public PostConnection getFeed(Integer first, String after) {
        Pageable pageable = PageRequest.of(0, first+1);

        List<Post> posts;

        if(after == null) {
            posts = postRepository.findAllByOrderByCreatedAtDescIdDesc(pageable);
        } else {
            DecodedCursor cursor = decodedCursor(after);
            posts = postRepository.findFeedBefore(cursor.createdAt(), cursor.id(), pageable);
        }

        boolean hasNextPage = posts.size() > first;
        List<Post> pageItems = hasNextPage ? posts.subList(0, first) : posts;

        List<PostEdge> edges = pageItems.stream()
                .map(post -> new PostEdge(post, encodeCursor(post.getCreatedAt(), post.getId())))
                .toList();

        String endCursor = edges.isEmpty() ? null : edges.getLast().cursor();
        PageInfo info = new PageInfo(hasNextPage, endCursor);

        return new PostConnection(info, edges);
    }
}
