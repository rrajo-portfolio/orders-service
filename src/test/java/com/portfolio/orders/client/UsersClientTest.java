package com.portfolio.orders.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio.orders.exception.RemoteResourceNotFoundException;
import java.io.IOException;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class UsersClientTest {

    private MockWebServer server;
    private UsersClient client;
    private UUID userId;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        WebClient webClient = WebClient.builder()
            .baseUrl(server.url("/").toString())
            .build();
        client = new UsersClient(webClient);
        userId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void existsReturnsTrueWhenUserIsPresent() {
        server.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("{\"exists\":true}"));

        assertThat(client.exists(userId)).isTrue();
    }

    @Test
    void fetchUserReturnsBody() {
        server.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {"id":"%s","fullName":"Portfolio User","email":"portfolio@example.com"}
                """.formatted(userId)));

        UsersClient.UserResponse response = client.fetchUser(userId);

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.fullName()).isEqualTo("Portfolio User");
    }

    @Test
    void fetchUserThrowsWhenNotFound() {
        server.enqueue(new MockResponse().setResponseCode(404));

        assertThatThrownBy(() -> client.fetchUser(userId))
            .isInstanceOf(RemoteResourceNotFoundException.class);
    }
}
