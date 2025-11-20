package com.portfolio.orders.service;

import com.portfolio.orders.entity.OrderEntity;
import com.portfolio.orders.entity.OrderItemEntity;
import com.portfolio.orders.entity.OrderStatus;
import com.portfolio.orders.generated.model.CreateOrderItem;
import com.portfolio.orders.generated.model.CreateOrderRequest;
import com.portfolio.orders.generated.model.Order;
import com.portfolio.orders.generated.model.OrderItem;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, OffsetDateTime.class, OrderStatus.class, BigDecimal.class})
public interface OrderMapper {

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "status", expression = "java(OrderStatus.PENDING)")
    @Mapping(target = "createdAt", expression = "java(OffsetDateTime.now())")
    @Mapping(target = "totalAmount", expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "version", constant = "0L")
    @Mapping(target = "userFullName", ignore = true)
    @Mapping(target = "userEmail", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderEntity toEntity(CreateOrderRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "price", ignore = true)
    @Mapping(target = "title", ignore = true)
    OrderItemEntity toItemEntity(CreateOrderItem item);

    @Mapping(target = "userName", source = "userFullName")
    Order toOrder(OrderEntity entity);

    @Mapping(target = "productName", source = "title")
    OrderItem toOrderItem(OrderItemEntity entity);

    List<OrderItem> toOrderItems(List<OrderItemEntity> items);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "userFullName", ignore = true)
    @Mapping(target = "userEmail", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(CreateOrderRequest request, @MappingTarget OrderEntity entity);

    @AfterMapping
    default void touchAudit(@MappingTarget OrderEntity entity) {
        entity.setUpdatedAt(OffsetDateTime.now());
    }
}
