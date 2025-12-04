package com.portfolio.orders.service;

import com.portfolio.orders.client.CatalogClient;
import com.portfolio.orders.client.UsersClient;
import com.portfolio.orders.entity.OrderEntity;
import com.portfolio.orders.entity.OrderItemEntity;
import com.portfolio.orders.entity.OrderStatus;
import com.portfolio.orders.exception.ConflictException;
import com.portfolio.orders.exception.RemoteResourceNotFoundException;
import com.portfolio.orders.exception.ResourceNotFoundException;
import com.portfolio.orders.events.OrderKafkaEventPublisher;
import com.portfolio.orders.events.OrderNotificationPublisher;
import com.portfolio.orders.generated.model.CreateOrderItem;
import com.portfolio.orders.generated.model.CreateOrderRequest;
import com.portfolio.orders.generated.model.Order;
import com.portfolio.orders.generated.model.OrderPage;
import com.portfolio.orders.generated.model.OrderStatusRequest;
import com.portfolio.orders.generated.model.UpdateOrderRequest;
import com.portfolio.orders.metrics.OrdersMetrics;
import com.portfolio.orders.repository.OrderRepository;
import com.portfolio.orders.security.SecurityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String[] ADMIN_AUTHORITIES = {
        "ROLE_portfolio_admin",
        "ROLE_admin",
        "ROLE_orders-admin",
        "ROLE_catalog_admin"
    };

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final UsersClient usersClient;
    private final CatalogClient catalogClient;
    private final OrderNotificationPublisher notificationPublisher;
    private final OrderKafkaEventPublisher kafkaEventPublisher;
    private final SecurityFacade securityFacade;
    private final OrdersMetrics ordersMetrics;

    @Transactional(readOnly = true)
    public OrderPage listOrders(Integer page, Integer size, String status) {
        if (!isPrivilegedUser()) {
            UUID currentUser = securityFacade.getCurrentUserId();
            if (currentUser == null) {
                throw new AccessDeniedException("Anonymous user cannot list orders");
            }
            List<Order> orders = repository.findByUserId(currentUser).stream()
                .filter(entity -> status == null || status.isBlank()
                    || entity.getStatus() == OrderStatus.valueOf(status))
                .map(mapper::toOrder)
                .toList();
            return new OrderPage()
                .content(orders)
                .page(0)
                .size(orders.size())
                .totalElements((long) orders.size())
                .totalPages(orders.isEmpty() ? 0 : 1);
        }

        Pageable pageable = PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
            Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderEntity> result;
        if (status != null && !status.isBlank()) {
            OrderStatus requestedStatus = OrderStatus.valueOf(status);
            result = repository.findAll((root, query, cb) -> cb.equal(root.get("status"), requestedStatus), pageable);
        } else {
            result = repository.findAll(pageable);
        }

        return new OrderPage()
            .content(result.stream().map(mapper::toOrder).toList())
            .page(result.getNumber())
            .size(result.getSize())
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages());
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        UUID requestedUserId = request.getUserId();
        UUID effectiveUserId = requestedUserId;
        if (!isPrivilegedUser()) {
            effectiveUserId = securityFacade.getCurrentUserId();
            if (effectiveUserId == null) {
                throw new AccessDeniedException("Unable to determine current user");
            }
        }
        boolean isNewCustomer = !repository.existsByUserId(effectiveUserId);
        UsersClient.UserResponse user = usersClient.fetchUser(effectiveUserId);

        OrderEntity entity = mapper.toEntity(request);
        entity.setUserId(effectiveUserId);
        entity.setUserFullName(user.fullName());
        entity.setUserEmail(user.email());
        entity.setCurrency(request.getCurrency());

        entity.clearItems();
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderItem item : request.getItems()) {
            UUID productId = item.getProductId();
            CatalogClient.CatalogProduct product = catalogClient.fetchProduct(productId);
            if (!product.currency().equalsIgnoreCase(request.getCurrency())) {
                throw new ConflictException("Product %s currency mismatch: expected %s, got %s"
                    .formatted(productId, request.getCurrency(), product.currency()));
            }
            OrderItemEntity itemEntity = mapper.toItemEntity(item);
            itemEntity.setPrice(product.price());
            itemEntity.setTitle(product.name());
            entity.addItem(itemEntity);
            total = total.add(product.price().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        entity.setTotalAmount(total);

        OrderEntity saved = repository.save(entity);
        notificationPublisher.publish(saved);
        kafkaEventPublisher.publish(saved);
        ordersMetrics.trackNewOrder(saved, isNewCustomer);
        return mapper.toOrder(saved);
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        OrderEntity entity = findById(id);
        assertCanAccess(entity);
        return mapper.toOrder(entity);
    }

    @Transactional
    public Order updateOrder(UUID id, UpdateOrderRequest request) {
        if (!isPrivilegedUser()) {
            throw new AccessDeniedException("Only administrators can update orders");
        }
        OrderEntity entity = findById(id);
        if (entity.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Cannot update a cancelled order");
        }

        entity.setNotes(request.getNotes());
        entity.clearItems();
        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderItem item : request.getItems()) {
            UUID productId = item.getProductId();
            CatalogClient.CatalogProduct product = catalogClient.fetchProduct(productId);
            OrderItemEntity itemEntity = mapper.toItemEntity(item);
            itemEntity.setPrice(product.price());
            itemEntity.setTitle(product.name());
            entity.addItem(itemEntity);
            total = total.add(product.price().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        entity.setTotalAmount(total);
        OrderEntity saved = repository.save(entity);
        notificationPublisher.publish(saved);
        return mapper.toOrder(saved);
    }

    @Transactional
    public void cancelOrder(UUID id) {
        if (!isPrivilegedUser()) {
            throw new AccessDeniedException("Only administrators can cancel orders");
        }
        OrderEntity entity = findById(id);
        entity.setStatus(OrderStatus.CANCELLED);
        OrderEntity saved = repository.save(entity);
        notificationPublisher.publish(saved);
        ordersMetrics.incrementStatus(OrderStatus.CANCELLED);
    }

    public void handlePaymentResult(UUID orderId, boolean success) {
        OrderEntity order = findById(orderId);
        if (success) {
            order.setStatus(OrderStatus.CONFIRMED);
            log.info("Order {} confirmed via payment saga", orderId);
        } else {
            order.setStatus(OrderStatus.CANCELLED);
            log.info("Order {} cancelled via payment saga", orderId);
        }
        repository.save(order);
        notificationPublisher.publish(order); // Notify user of update
    }

    @Transactional
    public Order updateStatus(UUID id, OrderStatusRequest request) {
        if (!isPrivilegedUser()) {
            throw new AccessDeniedException("Only administrators can update order status");
        }
        OrderEntity entity = findById(id);
        OrderStatus newStatus = OrderStatus.valueOf(request.getStatus().getValue());
        entity.setStatus(newStatus);
        OrderEntity saved = repository.save(entity);
        notificationPublisher.publish(saved);
        ordersMetrics.incrementStatus(newStatus);
        return mapper.toOrder(saved);
    }

    @Transactional(readOnly = true)
    public List<Order> listOrdersByUser(UUID userId) {
        if (!isPrivilegedUser()) {
            securityFacade.assertCurrentUser(userId);
        }
        return repository.findByUserId(userId).stream()
            .map(mapper::toOrder)
            .toList();
    }

    private OrderEntity findById(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order %s not found".formatted(id)));
    }

    private void assertCanAccess(OrderEntity entity) {
        if (isPrivilegedUser()) {
            return;
        }
        UUID currentUser = securityFacade.getCurrentUserId();
        if (currentUser == null || !currentUser.equals(entity.getUserId())) {
            throw new AccessDeniedException("User is not allowed to access this order");
        }
    }

    private boolean isPrivilegedUser() {
        return securityFacade.hasAnyAuthority(ADMIN_AUTHORITIES);
    }
}
