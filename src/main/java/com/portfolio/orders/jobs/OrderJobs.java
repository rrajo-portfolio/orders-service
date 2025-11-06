package com.portfolio.orders.jobs;

import com.portfolio.orders.entity.OrderStatus;
import com.portfolio.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderJobs {

    private static final Logger log = LoggerFactory.getLogger(OrderJobs.class);

    private final OrderRepository repository;

    /**
     * Emits metrics about pending orders every 15 minutes. Useful talking point for observability.
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void logPendingOrders() {
        long pending = repository.countByStatus(OrderStatus.PENDING);
        log.info("Order backlog - pending orders: {}", pending);
    }
}
