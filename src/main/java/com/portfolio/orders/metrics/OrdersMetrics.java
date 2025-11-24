package com.portfolio.orders.metrics;

import com.portfolio.orders.entity.OrderEntity;
import com.portfolio.orders.entity.OrderItemEntity;
import com.portfolio.orders.entity.OrderStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class OrdersMetrics {

    private final Map<OrderStatus, Counter> statusCounters = new EnumMap<>(OrderStatus.class);
    private final Counter newCustomerCounter;
    private final Counter returningCustomerCounter;
    private final Map<String, Counter> revenueByCurrency = new ConcurrentHashMap<>();
    private final Map<UUID, Counter> productQuantityCounters = new ConcurrentHashMap<>();
    private final MeterRegistry registry;

    public OrdersMetrics(MeterRegistry registry) {
        this.registry = registry;
        for (OrderStatus status : OrderStatus.values()) {
            statusCounters.put(
                status,
                Counter.builder("orders_status")
                    .tag("status", status.name())
                    .description("Total orders processed per status")
                    .register(registry)
            );
        }
        this.newCustomerCounter = Counter.builder("orders_customers")
            .tag("segment", "new")
            .description("Orders placed by new customers")
            .register(registry);
        this.returningCustomerCounter = Counter.builder("orders_customers")
            .tag("segment", "returning")
            .description("Orders placed by returning customers")
            .register(registry);
    }

    public void trackNewOrder(OrderEntity entity, boolean isNewCustomer) {
        incrementStatus(entity.getStatus());
        incrementRevenue(entity.getCurrency(), entity.getTotalAmount());
        incrementCustomerSegment(isNewCustomer);
        entity.getItems().forEach(this::incrementProductQuantity);
    }

    public void incrementStatus(OrderStatus status) {
        statusCounters.get(status).increment();
    }

    private void incrementCustomerSegment(boolean isNewCustomer) {
        if (isNewCustomer) {
            newCustomerCounter.increment();
        } else {
            returningCustomerCounter.increment();
        }
    }

    private void incrementRevenue(String currency, BigDecimal amount) {
        if (currency == null || amount == null) {
            return;
        }
        Counter counter = revenueByCurrency.computeIfAbsent(
            currency.toUpperCase(),
            key -> Counter.builder("orders_revenue")
                .tag("currency", key)
                .description("Aggregated order revenue by currency")
                .register(registry)
        );
        counter.increment(amount.doubleValue());
    }

    private void incrementProductQuantity(OrderItemEntity item) {
        Counter counter = productQuantityCounters.computeIfAbsent(
            item.getProductId(),
            id -> Counter.builder("orders_product_quantity")
                .tag("product_id", id.toString())
                .tag("product_name", item.getTitle() == null ? "unknown" : item.getTitle())
                .description("Total quantity sold per product")
                .register(registry)
        );
        counter.increment(item.getQuantity() == null ? 0 : item.getQuantity());
    }
}
