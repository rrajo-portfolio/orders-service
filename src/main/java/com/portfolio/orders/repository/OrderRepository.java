package com.portfolio.orders.repository;

import com.portfolio.orders.entity.OrderEntity;
import com.portfolio.orders.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID>, JpaSpecificationExecutor<OrderEntity> {

    List<OrderEntity> findByUserId(UUID userId);

    long countByStatus(OrderStatus status);
}
