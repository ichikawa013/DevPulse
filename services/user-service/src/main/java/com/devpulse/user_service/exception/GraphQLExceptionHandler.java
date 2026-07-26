package com.devpulse.user_service.exception;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class GraphQLExceptionHandler implements DataFetcherExceptionResolver {


    @Override
    public Mono<List<GraphQLError>> resolveException(Throwable exception, DataFetchingEnvironment environment) {

        if(exception instanceof BadRequestHandler e) {
            return Mono.just(List.of(
                    GraphqlErrorBuilder.newError(environment)
                            .message(e.getMessage())
                            .errorType(ErrorType.BAD_REQUEST)
                            .build()
            ));
        }

        if (exception instanceof ResourceNotFoundHandler e) {
            return Mono.just(List.of(
                    GraphqlErrorBuilder.newError(environment)
                            .message(e.getMessage())
                            .errorType(ErrorType.NOT_FOUND)
                            .build()
            ));
        }

        if (exception instanceof BadCredentialsException) {
            return Mono.just(List.of(
                    GraphqlErrorBuilder.newError(environment)
                            .message("Invalid email or password")
                            .errorType(ErrorType.UNAUTHORIZED)
                            .build()
            ));
        }

        return Mono.empty(); // let Spring handle unknown exceptions
    }
}
