package com.devpulse.feed_service.services;

import com.devpulse.feed_service.entities.Post;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class PostSubscriptionService {
    // TODO: revisit backpressure strategy — currently onBackpressureBuffer() (unbounded).
    // A subscriber that stops consuming (dead/slow WebSocket) will buffer forever, unbounded memory growth.
    // Look into onBackpressureDrop() / onBackpressureLatest() / bounded buffer(maxSize) tradeoffs before prod.
    // Docs: https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Sinks.MulticastSpec.html
    private final Sinks.Many<Post> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void emit(Post post) {
        sink.tryEmitNext(post);
    }

    public Flux<Post> stream() {
        return sink.asFlux();
    }
}
