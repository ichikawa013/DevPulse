package com.devpulse.feed_service.config;

import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.server.WebSocketGraphQlInterceptor;
import org.springframework.graphql.server.WebSocketSessionInfo;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

import java.util.Map;

public class WebSocketAuthInterceptor implements WebSocketGraphQlInterceptor {

    @Override
    public Mono<Object> handleConnectionInitialization(WebSocketSessionInfo sessionInfo,
                                                       Map<String, Object> connectionInitPayload) {
        Object userId = connectionInitPayload.get("X-User-Id");
        sessionInfo.getAttributes().put("X-User-Id", userId);
        return Mono.empty();
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(@NonNull WebGraphQlRequest request, @NonNull Chain chain) {
        Object userId = request.getAttributes().get("X-User-Id");
        if (userId != null) {
            request.configureExecutionInput((executionInput, builder) ->
                    builder.graphQLContext(ctx -> ctx.put("X-User-Id", userId)).build()
            );
        }
        return chain.next(request);
    }
}