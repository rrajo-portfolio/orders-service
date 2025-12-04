package com.portfolio.orders.events;

import com.portfolio.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResultListener {

    private final OrderService orderService;

    @KafkaListener(topics = "payment-results", groupId = "orders-group")
    public void handlePaymentResult(PaymentResultEvent event) {
        log.info("Received payment result for order: {}", event.orderId());
        boolean success = "AUTHORIZED".equals(event.status());
        try {
            orderService.handlePaymentResult(event.orderId(), success);
        } catch (Exception e) {
            log.error("Failed to update order status for order: {}", event.orderId(), e);
        }
    }
}
