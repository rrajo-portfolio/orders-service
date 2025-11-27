package com.portfolio.orders.client;

import com.portfolio.orders.exception.RemoteResourceNotFoundException;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);
    private static final String PRODUCT_NOT_FOUND_TEMPLATE = "Product %s not found";

    private final WebClient catalogWebClient;

    public CatalogClient(@Qualifier("catalogWebClient") WebClient catalogWebClient) {
        this.catalogWebClient = catalogWebClient;
    }

    public CatalogProduct fetchProduct(UUID productId) {
        return catalogWebClient.get()
            .uri("/products/{id}", productId)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response -> {
                log.warn("Product {} not found in catalog-service", productId);
                return Mono.error(new RemoteResourceNotFoundException(PRODUCT_NOT_FOUND_TEMPLATE.formatted(productId)));
            })
            .bodyToMono(CatalogProduct.class)
            .blockOptional()
            .orElseThrow(() -> new RemoteResourceNotFoundException(PRODUCT_NOT_FOUND_TEMPLATE.formatted(productId)));
    }

    public record CatalogProduct(UUID id, String name, String sku, BigDecimal price, String currency) {
    }
}
