package com.portfolio.orders.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.orders.entity.OrderEntity;
import com.portfolio.orders.entity.OrderItemEntity;
import com.portfolio.orders.entity.OrderStatus;
import com.portfolio.orders.generated.model.CreateOrderItem;
import com.portfolio.orders.generated.model.CreateOrderRequest;
import com.portfolio.orders.generated.model.Order;
import com.portfolio.orders.generated.model.OrderItem;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OrderMapperTest {

    private OrderMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(OrderMapper.class);
    }

    @Test
    void toEntityInitializesDefaults() {
        CreateOrderRequest request = new CreateOrderRequest()
            .userId(UUID.randomUUID())
            .currency("EUR")
            .notes("Testing")
            .items(List.of(new CreateOrderItem().productId(UUID.randomUUID()).quantity(1)));

        OrderEntity entity = mapper.toEntity(request);

        assertThat(entity.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(entity.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(entity.getItems()).hasSize(1);
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    void toItemEntityCopiesFields() {
        UUID productId = UUID.randomUUID();
        CreateOrderItem item = new CreateOrderItem().productId(productId).quantity(3);

        OrderItemEntity entity = mapper.toItemEntity(item);

        assertThat(entity.getProductId()).isEqualTo(productId);
        assertThat(entity.getQuantity()).isEqualTo(3);
    }

    @Test
    void toOrderConvertsEntity() {
        UUID id = UUID.randomUUID();
        OrderEntity entity = OrderEntity.builder()
            .id(id)
            .userId(UUID.randomUUID())
            .userFullName("Portfolio User")
            .status(OrderStatus.PENDING)
            .currency("USD")
            .createdAt(OffsetDateTime.now())
            .totalAmount(BigDecimal.TEN)
            .build();

        Order order = mapper.toOrder(entity);

        assertThat(order.getId()).isEqualTo(id);
        assertThat(order.getUserName()).isEqualTo("Portfolio User");
        assertThat(order.getStatus()).isEqualTo(com.portfolio.orders.generated.model.OrderStatus.PENDING);
    }

    @Test
    void updateEntityIgnoresRestrictedFields() {
        UUID id = UUID.randomUUID();
        OrderEntity entity = OrderEntity.builder()
            .id(id)
            .userId(UUID.randomUUID())
            .status(OrderStatus.PENDING)
            .currency("EUR")
            .totalAmount(BigDecimal.ZERO)
            .createdAt(OffsetDateTime.now())
            .build();

        CreateOrderRequest request = new CreateOrderRequest()
            .userId(entity.getUserId())
            .currency("USD")
            .notes("Updated")
            .items(List.of(new CreateOrderItem().productId(UUID.randomUUID()).quantity(2)));

        mapper.updateEntity(request, entity);

        assertThat(entity.getCurrency()).isEqualTo("USD");
        assertThat(entity.getNotes()).isEqualTo("Updated");
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    void toOrderItemConvertsItemEntity() {
        OrderItemEntity entity = OrderItemEntity.builder()
            .productId(UUID.randomUUID())
            .quantity(2)
            .title("Demo")
            .price(BigDecimal.valueOf(5))
            .build();

        OrderItem dto = mapper.toOrderItem(entity);

        assertThat(dto.getProductName()).isEqualTo("Demo");
        assertThat(dto.getQuantity()).isEqualTo(2);
    }
}

