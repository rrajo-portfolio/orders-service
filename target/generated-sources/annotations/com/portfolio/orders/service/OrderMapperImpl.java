package com.portfolio.orders.service;

import com.portfolio.orders.entity.OrderEntity;
import com.portfolio.orders.entity.OrderItemEntity;
import com.portfolio.orders.entity.OrderStatus;
import com.portfolio.orders.generated.model.CreateOrderItem;
import com.portfolio.orders.generated.model.CreateOrderRequest;
import com.portfolio.orders.generated.model.Order;
import com.portfolio.orders.generated.model.OrderItem;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-24T01:05:47+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Oracle Corporation)"
)
@Component
public class OrderMapperImpl implements OrderMapper {

    @Override
    public OrderEntity toEntity(CreateOrderRequest request) {
        if ( request == null ) {
            return null;
        }

        OrderEntity.OrderEntityBuilder orderEntity = OrderEntity.builder();

        orderEntity.userId( request.getUserId() );
        orderEntity.currency( request.getCurrency() );
        orderEntity.notes( request.getNotes() );
        orderEntity.items( createOrderItemListToOrderItemEntityList( request.getItems() ) );

        orderEntity.id( UUID.randomUUID() );
        orderEntity.status( OrderStatus.PENDING );
        orderEntity.createdAt( OffsetDateTime.now() );
        orderEntity.totalAmount( BigDecimal.ZERO );
        orderEntity.version( (long) 0L );

        return orderEntity.build();
    }

    @Override
    public OrderItemEntity toItemEntity(CreateOrderItem item) {
        if ( item == null ) {
            return null;
        }

        OrderItemEntity.OrderItemEntityBuilder orderItemEntity = OrderItemEntity.builder();

        orderItemEntity.productId( item.getProductId() );
        orderItemEntity.quantity( item.getQuantity() );

        return orderItemEntity.build();
    }

    @Override
    public Order toOrder(OrderEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Order order = new Order();

        order.setUserName( entity.getUserFullName() );
        order.setId( entity.getId() );
        order.setUserId( entity.getUserId() );
        order.setUserEmail( entity.getUserEmail() );
        order.setStatus( orderStatusToOrderStatus( entity.getStatus() ) );
        if ( entity.getTotalAmount() != null ) {
            order.setTotalAmount( entity.getTotalAmount().doubleValue() );
        }
        order.setCurrency( entity.getCurrency() );
        order.setNotes( entity.getNotes() );
        order.setCreatedAt( entity.getCreatedAt() );
        order.setUpdatedAt( entity.getUpdatedAt() );
        order.setItems( toOrderItems( entity.getItems() ) );

        return order;
    }

    @Override
    public OrderItem toOrderItem(OrderItemEntity entity) {
        if ( entity == null ) {
            return null;
        }

        OrderItem orderItem = new OrderItem();

        orderItem.setProductName( entity.getTitle() );
        orderItem.setProductId( entity.getProductId() );
        orderItem.setQuantity( entity.getQuantity() );
        if ( entity.getPrice() != null ) {
            orderItem.setPrice( entity.getPrice().doubleValue() );
        }
        orderItem.setTitle( entity.getTitle() );

        return orderItem;
    }

    @Override
    public List<OrderItem> toOrderItems(List<OrderItemEntity> items) {
        if ( items == null ) {
            return null;
        }

        List<OrderItem> list = new ArrayList<OrderItem>( items.size() );
        for ( OrderItemEntity orderItemEntity : items ) {
            list.add( toOrderItem( orderItemEntity ) );
        }

        return list;
    }

    @Override
    public void updateEntity(CreateOrderRequest request, OrderEntity entity) {
        if ( request == null ) {
            return;
        }

        if ( request.getCurrency() != null ) {
            entity.setCurrency( request.getCurrency() );
        }
        if ( request.getNotes() != null ) {
            entity.setNotes( request.getNotes() );
        }
        if ( entity.getItems() != null ) {
            List<OrderItemEntity> list = createOrderItemListToOrderItemEntityList( request.getItems() );
            if ( list != null ) {
                entity.getItems().clear();
                entity.getItems().addAll( list );
            }
        }
        else {
            List<OrderItemEntity> list = createOrderItemListToOrderItemEntityList( request.getItems() );
            if ( list != null ) {
                entity.setItems( list );
            }
        }

        touchAudit( entity );
    }

    protected List<OrderItemEntity> createOrderItemListToOrderItemEntityList(List<CreateOrderItem> list) {
        if ( list == null ) {
            return null;
        }

        List<OrderItemEntity> list1 = new ArrayList<OrderItemEntity>( list.size() );
        for ( CreateOrderItem createOrderItem : list ) {
            list1.add( toItemEntity( createOrderItem ) );
        }

        return list1;
    }

    protected com.portfolio.orders.generated.model.OrderStatus orderStatusToOrderStatus(OrderStatus orderStatus) {
        if ( orderStatus == null ) {
            return null;
        }

        com.portfolio.orders.generated.model.OrderStatus orderStatus1;

        switch ( orderStatus ) {
            case PENDING: orderStatus1 = com.portfolio.orders.generated.model.OrderStatus.PENDING;
            break;
            case CONFIRMED: orderStatus1 = com.portfolio.orders.generated.model.OrderStatus.CONFIRMED;
            break;
            case SHIPPED: orderStatus1 = com.portfolio.orders.generated.model.OrderStatus.SHIPPED;
            break;
            case DELIVERED: orderStatus1 = com.portfolio.orders.generated.model.OrderStatus.DELIVERED;
            break;
            case CANCELLED: orderStatus1 = com.portfolio.orders.generated.model.OrderStatus.CANCELLED;
            break;
            default: throw new IllegalArgumentException( "Unexpected enum constant: " + orderStatus );
        }

        return orderStatus1;
    }
}
