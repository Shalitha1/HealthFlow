package com.pm.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.apigateway.dto.ApiError;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@Component
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(exception);
        }

        HttpStatus status;
        String errorCode;
        String message;

        if (exception instanceof AuthException) {
            status = HttpStatus.UNAUTHORIZED;
            errorCode = "AUTH_UNAUTHORIZED";
            message = exception.getMessage();
        } else if (exception instanceof ForbiddenException) {
            status = HttpStatus.FORBIDDEN;
            errorCode = "AUTH_FORBIDDEN";
            message = exception.getMessage();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = "GATEWAY_INTERNAL_ERROR";
            message = "The request could not be processed";
        }

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                errorCode,
                message,
                exchange.getRequest().getPath().value(),
                Map.of()
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(error);
        } catch (JsonProcessingException serializationFailure) {
            return Mono.error(serializationFailure);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(bytes)
        ));
    }
}
