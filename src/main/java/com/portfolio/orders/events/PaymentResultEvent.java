package com.portfolio.orders.events;

import java.util.UUID;

public record PaymentResultEvent(
    UUID orderId,
    String paymentId,
    String status
) {}
