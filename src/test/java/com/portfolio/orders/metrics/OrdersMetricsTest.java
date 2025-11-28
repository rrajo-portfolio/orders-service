package com.portfolio.orders.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.orders.entity.OrderEntity;
import com.portfolio.orders.entity.OrderItemEntity;
import com.portfolio.orders.entity.OrderStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrdersMetricsTest {

    private SimpleMeterRegistry registry;
    private OrdersMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new OrdersMetrics(registry);
    }

    @Test
    void trackNewOrderUpdatesAllCounters() {
        OrderEntity entity = OrderEntity.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .status(OrderStatus.CONFIRMED)
            .currency("usd")
            .totalAmount(BigDecimal.valueOf(125.50))
            .createdAt(OffsetDateTime.now())
            .items(new ArrayList<>())
            .build();
        OrderItemEntity item = OrderItemEntity.builder()
            .productId(UUID.randomUUID())
            .quantity(2)
            .price(BigDecimal.valueOf(40))
            .title("Portfolio Workshop")
            .build();
        entity.addItem(item);

        metrics.trackNewOrder(entity, true);

        assertThat(registry.get("orders_status").tag("status", "CONFIRMED").counter().count()).isEqualTo(1);
        assertThat(registry.get("orders_customers").tag("segment", "new").counter().count()).isEqualTo(1);
        assertThat(registry.get("orders_revenue").tag("currency", "USD").counter().count())
            .isEqualTo(125.50);
        assertThat(registry.get("orders_product_quantity")
            .tag("product_id", item.getProductId().toString())
            .tag("product_name", "Portfolio Workshop")
            .counter().count()).isEqualTo(2);
    }

    @Test
    void incrementStatusCountsReturningCustomers() {
        metrics.incrementStatus(OrderStatus.SHIPPED);
        metrics.trackNewOrder(
            OrderEntity.builder()
                .status(OrderStatus.SHIPPED)
                .currency("eur")
                .totalAmount(BigDecimal.ONE)
                .createdAt(OffsetDateTime.now())
                .items(new ArrayList<>())
                .build(),
            false
        );

        assertThat(registry.get("orders_status").tag("status", "SHIPPED").counter().count()).isEqualTo(2);
        assertThat(registry.get("orders_customers").tag("segment", "returning").counter().count()).isEqualTo(1);
    }
}
