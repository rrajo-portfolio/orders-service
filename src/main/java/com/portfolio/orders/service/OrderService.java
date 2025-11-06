package com.portfolio.orders.service;

import com.portfolio.orders.client.CatalogClient;
import com.portfolio.orders.client.UsersClient;
import com.portfolio.orders.entity.OrderEntity;
import com.portfolio.orders.entity.OrderItemEntity;
import com.portfolio.orders.entity.OrderStatus;
import com.portfolio.orders.exception.ConflictException;
import com.portfolio.orders.exception.RemoteResourceNotFoundException;
import com.portfolio.orders.exception.ResourceNotFoundException;
import com.portfolio.orders.generated.model.CreateOrderItem;
import com.portfolio.orders.generated.model.CreateOrderRequest;
import com.portfolio.orders.generated.model.Order;
import com.portfolio.orders.generated.model.OrderPage;
import com.portfolio.orders.generated.model.OrderStatusRequest;
import com.portfolio.orders.generated.model.UpdateOrderRequest;
import com.portfolio.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
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

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final UsersClient usersClient;
    private final CatalogClient catalogClient;

    @Transactional(readOnly = true)
    public OrderPage listOrders(Integer page, Integer size, String status) {
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
        UUID userId = request.getUserId();
        if (!usersClient.exists(userId)) {
            throw new RemoteResourceNotFoundException("User %s not found in users-service".formatted(userId));
        }
        OrderEntity entity = mapper.toEntity(request);
        entity.setUserId(userId);
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
        return mapper.toOrder(saved);
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        return mapper.toOrder(findById(id));
    }

    @Transactional
    public Order updateOrder(UUID id, UpdateOrderRequest request) {
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
        return mapper.toOrder(saved);
    }

    @Transactional
    public void cancelOrder(UUID id) {
        OrderEntity entity = findById(id);
        entity.setStatus(OrderStatus.CANCELLED);
        repository.save(entity);
    }

    @Transactional
    public Order updateStatus(UUID id, OrderStatusRequest request) {
        OrderEntity entity = findById(id);
        OrderStatus newStatus = OrderStatus.valueOf(request.getStatus().getValue());
        entity.setStatus(newStatus);
        return mapper.toOrder(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<Order> listOrdersByUser(UUID userId) {
        return repository.findByUserId(userId).stream()
            .map(mapper::toOrder)
            .toList();
    }

    private OrderEntity findById(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order %s not found".formatted(id)));
    }
}
