package com.portfolio.orders.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio.orders.exception.RemoteResourceNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class CatalogClientTest {

    private MockWebServer server;
    private CatalogClient client;
    private UUID productId;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        WebClient webClient = WebClient.builder()
            .baseUrl(server.url("/").toString())
            .build();
        client = new CatalogClient(webClient);
        productId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void fetchProductReturnsPayload() {
        server.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {"id":"%s","name":"Portfolio Review","sku":"PORT-01","price":19.99,"currency":"EUR"}
                """.formatted(productId)));

        CatalogClient.CatalogProduct product = client.fetchProduct(productId);

        assertThat(product.id()).isEqualTo(productId);
        assertThat(product.price()).isEqualByComparingTo(BigDecimal.valueOf(19.99));
        assertThat(product.currency()).isEqualTo("EUR");
    }

    @Test
    void fetchProductPropagatesNotFound() {
        server.enqueue(new MockResponse().setResponseCode(404));

        assertThatThrownBy(() -> client.fetchProduct(productId))
            .isInstanceOf(RemoteResourceNotFoundException.class);
    }
}
