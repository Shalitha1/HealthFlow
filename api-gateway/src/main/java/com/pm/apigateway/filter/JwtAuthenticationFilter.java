package com.pm.apigateway.filter;

import com.pm.apigateway.exception.AuthException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {
    private final WebClient webClient;
    private final String validateUrl;

    public JwtAuthenticationFilter(
            WebClient.Builder webClientBuilder,
            @Value("${auth-service.validate-url:http://auth-service:8085/auth/validate}") String validateUrl
    ) {
        super(Config.class);
        this.webClient = webClientBuilder.build();
        this.validateUrl = validateUrl;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authorizationHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throw new AuthException("Missing or invalid Authorization header");
            }

            String token = authorizationHeader.substring(7);

            return webClient.post()
                    .uri(validateUrl)
                    .bodyValue(Map.of("token", token))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            Mono.error(new AuthException("Invalid or expired token")))
                    .bodyToMono(Map.class)
                    .flatMap(claims -> chain.filter(exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .header("X-User-Id", String.valueOf(claims.get("sub")))
                                    .header("X-User-Role", String.valueOf(claims.get("role")))
                                    .build())
                            .build()))
                    .onErrorMap(ex -> ex instanceof AuthException
                            ? ex
                            : new AuthException("Token validation failed"));
        };
    }

    public static class Config {
    }
}
