package com.portfolio.orders.client;

import com.portfolio.orders.exception.RemoteResourceNotFoundException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class UsersClient {

    private static final Logger log = LoggerFactory.getLogger(UsersClient.class);

    private final WebClient usersWebClient;

    public UsersClient(@Qualifier("usersWebClient") WebClient usersWebClient) {
        this.usersWebClient = usersWebClient;
    }

    public boolean exists(UUID userId) {
        return usersWebClient.get()
            .uri("/users/{id}/exists", userId)
            .retrieve()
            .onStatus(status -> status.is4xxClientError(), response -> {
                log.warn("User {} validation failed with status {}", userId, response.statusCode());
                return Mono.error(new RemoteResourceNotFoundException("User %s not found".formatted(userId)));
            })
            .bodyToMono(UserExistsResponse.class)
            .map(UserExistsResponse::exists)
            .blockOptional()
            .orElse(false);
    }

    public record UserExistsResponse(boolean exists) {
    }
}
