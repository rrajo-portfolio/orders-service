package com.portfolio.orders.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.orders.entity.OrderEntity;
import com.portfolio.orders.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final OrderNotificationProperties properties;
    private final ObjectMapper objectMapper;

    public void publish(OrderEntity order) {
        if (!properties.enabled()) {
            return;
        }
        try {
            OrderStatus status = order.getStatus();
            OrderNotificationPayload payload = new OrderNotificationPayload(
                order.getId(),
                status != null ? status.name() : "UNKNOWN",
                order.getTotalAmount()
            );
            String body = objectMapper.writeValueAsString(payload);
            String routingKey = properties.routingKeyPattern().replace("*", status != null ? status.name().toLowerCase() : "unknown");
            rabbitTemplate.convertAndSend(properties.exchange(), routingKey, body);
            log.debug("Published order notification for {} with routing key {}", order.getId(), routingKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order {} notification payload", order.getId(), e);
        }
    }

    public record OrderNotificationPayload(
        java.util.UUID orderId,
        String status,
        java.math.BigDecimal totalAmount
    ) {
    }
}
