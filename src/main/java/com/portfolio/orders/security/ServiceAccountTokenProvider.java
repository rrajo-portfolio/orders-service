package com.portfolio.orders.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ServiceAccountTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(ServiceAccountTokenProvider.class);

    private final WebClient tokenClient;
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final String username;
    private final String password;
    private final String passwordClientId;
    private final AtomicReference<TokenHolder> currentToken = new AtomicReference<>();

    public ServiceAccountTokenProvider(
        @Value("${service-account.token-uri:http://keycloak:8080/auth/realms/portfolio/protocol/openid-connect/token}") String tokenUri,
        @Value("${service-account.client-id:portfolio-api}") String clientId,
        @Value("${service-account.client-secret:portfolio-api-secret}") String clientSecret,
        @Value("${service-account.username:}") String username,
        @Value("${service-account.password:}") String password,
        @Value("${service-account.password-client-id:portfolio-frontend}") String passwordClientId
    ) {
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = username;
        this.password = password;
        this.passwordClientId = passwordClientId;
        this.tokenClient = WebClient.builder().build();
    }

    public String getToken() {
        TokenHolder holder = currentToken.get();
        if (holder == null || holder.isExpired()) {
            synchronized (this) {
                holder = currentToken.get();
                if (holder == null || holder.isExpired()) {
                    holder = fetchToken();
                    currentToken.set(holder);
                }
            }
        }
        return holder.token();
    }

    private TokenHolder fetchToken() {
        try {
            return fetchClientCredentialsToken();
        } catch (Exception ex) {
            if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
                log.warn("Client credentials flow failed ({}). Falling back to password grant.", ex.getMessage());
                return fetchPasswordToken();
            }
            throw ex;
        }
    }

    private TokenHolder fetchClientCredentialsToken() {
        log.debug("Requesting service account token for {}", clientId);
        return exchangeForToken(
            BodyInserters
                .fromFormData("grant_type", "client_credentials")
                .with("client_id", clientId)
                .with("client_secret", clientSecret)
        );
    }

    private TokenHolder fetchPasswordToken() {
        log.debug("Requesting password grant token for {}", username);
        return exchangeForToken(
            BodyInserters
                .fromFormData("grant_type", "password")
                .with("client_id", passwordClientId)
                .with("username", username)
                .with("password", password)
        );
    }

    private TokenHolder exchangeForToken(BodyInserters.FormInserter<String> body) {
        Map<String, Object> response = tokenClient.post()
            .uri(tokenUri)
            .body(body)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        String accessToken = (String) response.get("access_token");
        Number expiresIn = (Number) response.getOrDefault("expires_in", 60);
        Instant expiresAt = Instant.now().plusSeconds(expiresIn.longValue() - 30);
        return new TokenHolder(accessToken, expiresAt);
    }

    private record TokenHolder(String token, Instant expiresAt) {
        boolean isExpired() {
            return token == null || expiresAt.isBefore(Instant.now());
        }
    }
}
