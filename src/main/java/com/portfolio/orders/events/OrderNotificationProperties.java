package com.portfolio.orders.events;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orders.notification")
public record OrderNotificationProperties(
    String exchange,
    String routingKeyPattern,
    boolean enabled
) {
}
