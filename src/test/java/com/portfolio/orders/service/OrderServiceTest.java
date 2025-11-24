package com.portfolio.orders.service;

import com.portfolio.orders.client.CatalogClient;
import com.portfolio.orders.client.UsersClient;
import com.portfolio.orders.entity.OrderEntity;
import com.portfolio.orders.entity.OrderItemEntity;
import com.portfolio.orders.entity.OrderStatus;
import com.portfolio.orders.events.OrderKafkaEventPublisher;
import com.portfolio.orders.events.OrderNotificationPublisher;
import com.portfolio.orders.exception.ConflictException;
import com.portfolio.orders.exception.RemoteResourceNotFoundException;
import com.portfolio.orders.exception.ResourceNotFoundException;
import com.portfolio.orders.generated.model.CreateOrderItem;
import com.portfolio.orders.generated.model.CreateOrderRequest;
import com.portfolio.orders.generated.model.Order;
import com.portfolio.orders.generated.model.OrderPage;
import com.portfolio.orders.generated.model.OrderStatusRequest;
import com.portfolio.orders.generated.model.UpdateOrderRequest;
import com.portfolio.orders.metrics.OrdersMetrics;
import com.portfolio.orders.repository.OrderRepository;
import com.portfolio.orders.security.SecurityFacade;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UsersClient usersClient;

    @Mock
    private CatalogClient catalogClient;

    @Mock
    private OrderNotificationPublisher notificationPublisher;

    @Mock
    private OrderKafkaEventPublisher kafkaEventPublisher;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private OrdersMetrics ordersMetrics;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private UUID productId;
    private CreateOrderRequest createOrderRequest;
    private OrderEntity baseEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        createOrderRequest = new CreateOrderRequest()
            .userId(userId)
            .currency("EUR")
            .items(List.of(new CreateOrderItem().productId(productId).quantity(2)));

        baseEntity = OrderEntity.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .status(OrderStatus.PENDING)
            .currency("EUR")
            .totalAmount(BigDecimal.ZERO)
            .createdAt(OffsetDateTime.now())
            .build();

        lenient().when(securityFacade.hasAnyAuthority(org.mockito.ArgumentMatchers.any(String[].class)))
            .thenReturn(true);
        lenient().when(securityFacade.getCurrentUserId()).thenReturn(userId);
        lenient().when(orderRepository.existsByUserId(any(UUID.class))).thenReturn(false);
    }

    @Test
    @DisplayName("createOrder should validate dependencies and persist order")
    void createOrderPersistsOrder() {
        UsersClient.UserResponse userResponse = new UsersClient.UserResponse(userId, "Roberto Portfolio", "roberto@example.com");
        when(usersClient.fetchUser(userId)).thenReturn(userResponse);
        when(orderMapper.toEntity(createOrderRequest)).thenReturn(baseEntity);
        when(orderMapper.toItemEntity(any(CreateOrderItem.class))).thenAnswer(invocation -> {
            CreateOrderItem item = invocation.getArgument(0);
            return OrderItemEntity.builder()
                .id(UUID.randomUUID())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(BigDecimal.ZERO)
                .build();
        });
        CatalogClient.CatalogProduct catalogProduct = new CatalogClient.CatalogProduct(productId, "Portfolio Review", "PORT-CAT", BigDecimal.valueOf(25), "EUR");
        when(catalogClient.fetchProduct(productId)).thenReturn(catalogProduct);
        when(orderRepository.save(baseEntity)).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toOrder(baseEntity)).thenAnswer(invocation -> new Order()
            .id(baseEntity.getId())
            .userId(baseEntity.getUserId())
            .currency(baseEntity.getCurrency())
            .totalAmount(baseEntity.getTotalAmount().doubleValue()));

        Order result = orderService.createOrder(createOrderRequest);

        assertThat(result.getTotalAmount()).isEqualTo(50.0);
        verify(orderRepository).save(baseEntity);
        verify(notificationPublisher).publish(baseEntity);
        verify(kafkaEventPublisher).publish(baseEntity);
        verify(ordersMetrics).trackNewOrder(baseEntity, true);
    }

    @Test
    @DisplayName("createOrder should throw when user does not exist")
    void createOrderMissingUserThrows() {
        when(usersClient.fetchUser(userId)).thenThrow(new RemoteResourceNotFoundException("User not found"));

        assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
            .isInstanceOf(RemoteResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(notificationPublisher, never()).publish(any());
        verify(kafkaEventPublisher, never()).publish(any());
        verify(ordersMetrics, never()).trackNewOrder(any(), anyBoolean());
    }

    @Test
    @DisplayName("updateStatus should change order state and persist")
    void updateStatusChangesState() {
        UUID orderId = UUID.randomUUID();
        OrderEntity entity = OrderEntity.builder()
            .id(orderId)
            .userId(userId)
            .status(OrderStatus.PENDING)
            .currency("EUR")
            .totalAmount(BigDecimal.TEN)
            .createdAt(OffsetDateTime.now())
            .build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(entity));
        when(orderRepository.save(entity)).thenReturn(entity);
        when(orderMapper.toOrder(entity)).thenReturn(new Order().id(orderId).status(com.portfolio.orders.generated.model.OrderStatus.CONFIRMED));

        OrderStatusRequest request = new OrderStatusRequest()
            .status(com.portfolio.orders.generated.model.OrderStatus.CONFIRMED);

        Order result = orderService.updateStatus(orderId, request);

        assertThat(entity.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getStatus()).isEqualTo(com.portfolio.orders.generated.model.OrderStatus.CONFIRMED);
        verify(notificationPublisher).publish(entity);
        verify(ordersMetrics).incrementStatus(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("updateOrder should throw when order is cancelled")
    void updateOrderCancelledThrowsConflict() {
        UUID orderId = UUID.randomUUID();
        OrderEntity entity = OrderEntity.builder()
            .id(orderId)
            .userId(userId)
            .status(OrderStatus.CANCELLED)
            .currency("EUR")
            .totalAmount(BigDecimal.TEN)
            .createdAt(OffsetDateTime.now())
            .build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(entity));

        UpdateOrderRequest request = new UpdateOrderRequest().items(List.of(new CreateOrderItem().productId(productId).quantity(1)));

        assertThatThrownBy(() -> orderService.updateOrder(orderId, request))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("getOrder should throw when not found")
    void getOrderNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listOrders should delegate to repository and map results")
    void listOrdersDelegatesToRepository() {
        OrderEntity entity = baseEntity;
        Page<OrderEntity> page = new PageImpl<>(List.of(entity));
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(orderMapper.toOrder(entity)).thenReturn(new Order().id(entity.getId()));

        OrderPage result = orderService.listOrders(0, 5, null);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("listOrders should filter by status when provided")
    void listOrdersFiltersByStatus() {
        Page<OrderEntity> page = new PageImpl<>(List.of(baseEntity));
        when(orderRepository.findAll(org.mockito.ArgumentMatchers.<Specification<OrderEntity>>any(), any(Pageable.class)))
            .thenReturn(page);
        when(orderMapper.toOrder(baseEntity)).thenReturn(new Order().id(baseEntity.getId()));

        OrderPage result = orderService.listOrders(0, 5, "PENDING");

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAll(org.mockito.ArgumentMatchers.<Specification<OrderEntity>>any(), any(Pageable.class));
    }

    @Test
    @DisplayName("listOrdersByUser should return mapped orders")
    void listOrdersByUserReturnsOrders() {
        when(orderRepository.findByUserId(userId)).thenReturn(List.of(baseEntity));
        when(orderMapper.toOrder(baseEntity)).thenReturn(new Order().id(baseEntity.getId()));

        List<Order> result = orderService.listOrdersByUser(userId);

        assertThat(result).hasSize(1);
        verify(orderRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("cancelOrder should mark order as cancelled")
    void cancelOrderMarksStatusCancelled() {
        UUID orderId = UUID.randomUUID();
        OrderEntity entity = baseEntity;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(entity));

        when(orderRepository.save(entity)).thenReturn(entity);

        orderService.cancelOrder(orderId);

        assertThat(entity.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(entity);
        verify(notificationPublisher).publish(entity);
        verify(ordersMetrics).incrementStatus(OrderStatus.CANCELLED);
    }
}
