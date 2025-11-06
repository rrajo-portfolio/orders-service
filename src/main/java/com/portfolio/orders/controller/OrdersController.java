package com.portfolio.orders.controller;

import com.portfolio.orders.generated.api.OrdersApi;
import com.portfolio.orders.generated.model.CreateOrderRequest;
import com.portfolio.orders.generated.model.Order;
import com.portfolio.orders.generated.model.OrderPage;
import com.portfolio.orders.generated.model.OrderStatus;
import com.portfolio.orders.generated.model.OrderStatusRequest;
import com.portfolio.orders.generated.model.UpdateOrderRequest;
import com.portfolio.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrdersController implements OrdersApi {

    private final OrderService orderService;

    @Override
    @PreAuthorize("hasAnyAuthority('SCOPE_orders.write','ROLE_orders_write','ROLE_orders-admin','ROLE_catalog_admin','ROLE_portfolio_admin')")
    public ResponseEntity<Order> createOrder(CreateOrderRequest createOrderRequest) {
        return ResponseEntity.status(201).body(orderService.createOrder(createOrderRequest));
    }

    @Override
    @PreAuthorize("hasAnyAuthority('SCOPE_orders.read','ROLE_orders_read','ROLE_orders-admin','ROLE_catalog_admin','ROLE_portfolio_admin')")
    public ResponseEntity<OrderPage> listOrders(Integer page, Integer size, OrderStatus status) {
        String statusValue = status != null ? status.getValue() : null;
        return ResponseEntity.ok(orderService.listOrders(page, size, statusValue));
    }

    @Override
    @PreAuthorize("hasAnyAuthority('SCOPE_orders.read','ROLE_orders_read','ROLE_orders-admin','ROLE_catalog_admin','ROLE_portfolio_admin')")
    public ResponseEntity<Order> getOrder(UUID id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @Override
    @PreAuthorize("hasAnyAuthority('SCOPE_orders.write','ROLE_orders_write','ROLE_orders-admin','ROLE_catalog_admin','ROLE_portfolio_admin')")
    public ResponseEntity<Order> updateOrder(UUID id, UpdateOrderRequest updateOrderRequest) {
        return ResponseEntity.ok(orderService.updateOrder(id, updateOrderRequest));
    }

    @Override
    @PreAuthorize("hasAnyAuthority('SCOPE_orders.write','ROLE_orders_write','ROLE_orders-admin','ROLE_catalog_admin','ROLE_portfolio_admin')")
    public ResponseEntity<Void> cancelOrder(UUID id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAnyAuthority('SCOPE_orders.write','ROLE_orders_write','ROLE_orders-admin','ROLE_catalog_admin','ROLE_portfolio_admin')")
    public ResponseEntity<Order> updateOrderStatus(UUID id, OrderStatusRequest orderStatusRequest) {
        return ResponseEntity.ok(orderService.updateStatus(id, orderStatusRequest));
    }

    @Override
    @PreAuthorize("hasAnyAuthority('SCOPE_orders.read','ROLE_orders_read','ROLE_orders-admin','ROLE_catalog_admin','ROLE_portfolio_admin')")
    public ResponseEntity<List<Order>> listOrdersByUser(UUID userId) {
        return ResponseEntity.ok(orderService.listOrdersByUser(userId));
    }
}
