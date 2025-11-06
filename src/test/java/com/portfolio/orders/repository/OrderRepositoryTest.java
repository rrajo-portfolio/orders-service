package com.portfolio.orders.repository;

import com.portfolio.orders.entity.OrderEntity;
import com.portfolio.orders.entity.OrderItemEntity;
import com.portfolio.orders.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:orders-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create"
})
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("findByUserId should return orders for given user")
    void findByUserIdReturnsOrders() {
        UUID userId = UUID.randomUUID();
        OrderEntity entity = baseOrder(userId);
        entity.addItem(orderItem(BigDecimal.TEN));
        orderRepository.save(entity);

        List<OrderEntity> result = orderRepository.findByUserId(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("countByStatus should return number of orders matching status")
    void countByStatusReturnsAmount() {
        OrderEntity pending = baseOrder(UUID.randomUUID());
        orderRepository.save(pending);

        OrderEntity shipped = baseOrder(UUID.randomUUID());
        shipped.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(shipped);

        long pendingCount = orderRepository.countByStatus(OrderStatus.PENDING);
        long shippedCount = orderRepository.countByStatus(OrderStatus.SHIPPED);

        assertThat(pendingCount).isEqualTo(1);
        assertThat(shippedCount).isEqualTo(1);
    }

    private OrderEntity baseOrder(UUID userId) {
        return OrderEntity.builder()
            .userId(userId)
            .status(OrderStatus.PENDING)
            .currency("EUR")
            .totalAmount(BigDecimal.ZERO)
            .notes("Awaiting confirmation")
            .createdAt(OffsetDateTime.now())
            .build();
    }

    private OrderItemEntity orderItem(BigDecimal price) {
        return OrderItemEntity.builder()
            .productId(UUID.randomUUID())
            .quantity(1)
            .price(price)
            .title("Portfolio SKU")
            .build();
    }
}
