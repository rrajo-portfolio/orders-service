package com.portfolio.orders.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.orders.entity.OrderEntity;
import com.portfolio.orders.entity.OrderItemEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderKafkaEventPublisher {

    @Value("${orders.kafka.topic:orders-checkout-events}")
    private String topic;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(OrderEntity order) {
        if (order == null) {
            return;
        }
        try {
            OrderKafkaPayload payload = new OrderKafkaPayload(
                order.getId(),
                order.getUserId(),
                order.getUserFullName(),
                order.getUserEmail(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCreatedAt(),
                order.getItems().stream()
                    .map(item -> new OrderKafkaItem(item.getProductId(), item.getTitle(), item.getQuantity(), item.getPrice()))
                    .toList()
            );
            String body = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, order.getId().toString(), body);
            log.debug("Published order {} event to Kafka topic {}", order.getId(), topic);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize order {} kafka payload", order.getId(), ex);
        }
    }

    public record OrderKafkaPayload(
        UUID orderId,
        UUID userId,
        String userName,
        String userEmail,
        String status,
        BigDecimal totalAmount,
        String currency,
        OffsetDateTime createdAt,
        List<OrderKafkaItem> items
    ) {
    }

    public record OrderKafkaItem(
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal price
    ) {
    }
}
