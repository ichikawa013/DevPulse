package com.devpulse.feed_service.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.NoSuchElementException;

@Component
public class GraphQLExceptionHandler implements DataFetcherExceptionResolver {

    @Override
    public Mono<List<GraphQLError>> resolveException(Throwable exception, DataFetchingEnvironment environment) {

        if (exception instanceof AccessDeniedException) {
            return Mono.just(List.of(
                    GraphqlErrorBuilder.newError(environment)
                            .message(exception.getMessage())
                            .errorType(ErrorType.FORBIDDEN)
                            .build()
            ));
        }

        if (exception instanceof NoSuchElementException) {
            return Mono.just(List.of(
                    GraphqlErrorBuilder.newError(environment)
                            .message(exception.getMessage())
                            .errorType(ErrorType.NOT_FOUND)
                            .build()
            ));
        }

        return Mono.empty(); // let Spring handle unknown exceptions
    }
}