package com.devpulse.notification_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;

@Configuration
public class WebGraphQLConfig {

    @Bean
    public WebGraphQlInterceptor headerInterceptor() {
        return (WebGraphQlRequest request, WebGraphQlInterceptor.Chain chain) -> {
            String userId = request.getHeaders().getFirst("X-User-Id");
            if (userId != null) {
                request.configureExecutionInput((executionInput, builder) ->
                        builder.graphQLContext(contextBuilder -> contextBuilder.put("X-User-Id", userId)).build()
                );
            }
            return chain.next(request);
        };
    }
}