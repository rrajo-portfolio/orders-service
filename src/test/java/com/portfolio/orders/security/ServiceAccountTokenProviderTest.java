package com.portfolio.orders.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceAccountTokenProviderTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void cachesTokenUntilExpiration() {
        server.enqueue(tokenResponse("cached-token", 3600));

        ServiceAccountTokenProvider provider = new ServiceAccountTokenProvider(
            server.url("/token").toString(),
            "client-id",
            "secret",
            "",
            "",
            "frontend"
        );

        assertThat(provider.getToken()).isEqualTo("cached-token");
        assertThat(provider.getToken()).isEqualTo("cached-token");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void fallsBackToPasswordGrantWhenClientCredentialsFails() {
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(tokenResponse("password-token", 120));

        ServiceAccountTokenProvider provider = new ServiceAccountTokenProvider(
            server.url("/token").toString(),
            "client-id",
            "secret",
            "portfolio-user",
            "portfolio-password",
            "frontend"
        );

        assertThat(provider.getToken()).isEqualTo("password-token");
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    private MockResponse tokenResponse(String tokenValue, long expiresInSeconds) {
        return new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {"access_token":"%s","expires_in":%d}
                """.formatted(tokenValue, expiresInSeconds));
    }
}
