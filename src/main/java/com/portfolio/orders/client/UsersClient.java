package com.portfolio.orders.client;

import com.portfolio.orders.exception.RemoteResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class UsersClient {

    private static final Logger log = LoggerFactory.getLogger(UsersClient.class);
    private static final String USER_NOT_FOUND_TEMPLATE = "User %s not found";

    private final WebClient usersWebClient;

    public UsersClient(@Qualifier("usersWebClient") WebClient usersWebClient) {
        this.usersWebClient = usersWebClient;
    }

    @CircuitBreaker(name = "users", fallbackMethod = "existsFallback")
    public boolean exists(UUID userId) {
        return usersWebClient.get()
            .uri("/users/{id}/exists", userId)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response -> {
                log.warn("User {} validation failed with status {}", userId, response.statusCode());
                return Mono.error(new RemoteResourceNotFoundException(USER_NOT_FOUND_TEMPLATE.formatted(userId)));
            })
            .bodyToMono(UserExistsResponse.class)
            .map(UserExistsResponse::exists)
            .blockOptional()
            .orElse(false);
    }

    public boolean existsFallback(UUID userId, Throwable t) {
        log.error("Fallback: Failed to check user existence {} from users-service. Reason: {}", userId, t.getMessage());
        // Fail safe: assume user exists? Or fail secure?
        // For high value orders, fail secure. For now, let's throw.
        throw new RuntimeException("Users service is unavailable. Please try again later.");
    }

    @CircuitBreaker(name = "users", fallbackMethod = "fetchUserFallback")
    public UserResponse fetchUser(UUID userId) {
        return usersWebClient.get()
            .uri("/users/{id}", userId)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response -> {
                log.warn("User {} fetch failed with status {}", userId, response.statusCode());
                return Mono.error(new RemoteResourceNotFoundException(USER_NOT_FOUND_TEMPLATE.formatted(userId)));
            })
            .bodyToMono(UserResponse.class)
            .blockOptional()
            .orElseThrow(() -> new RemoteResourceNotFoundException(USER_NOT_FOUND_TEMPLATE.formatted(userId)));
    }

    public UserResponse fetchUserFallback(UUID userId, Throwable t) {
        log.error("Fallback: Failed to fetch user {} from users-service. Reason: {}", userId, t.getMessage());
        throw new RuntimeException("Users service is unavailable. Please try again later.");
    }

    public record UserExistsResponse(boolean exists) {
    }

    public record UserResponse(UUID id, String fullName, String email) {
    }
}
