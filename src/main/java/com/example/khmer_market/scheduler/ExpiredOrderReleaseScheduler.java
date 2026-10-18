package com.example.khmer_market.scheduler;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.repository.OrderRepository;
import com.example.khmer_market.service.OrderStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ExpiredOrderReleaseScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiredOrderReleaseScheduler.class);

    private final OrderRepository orderRepository;
    private final OrderStateMachine orderStateMachine;

    @Value("${market.business.order-reservation-timeout-minutes:30}")
    private int timeoutMinutes = 30;

    public ExpiredOrderReleaseScheduler(OrderRepository orderRepository,
                                        OrderStateMachine orderStateMachine) {
        this.orderRepository = orderRepository;
        this.orderStateMachine = orderStateMachine;
    }

    @Scheduled(fixedDelay = 60000) // Run every 60 seconds
    public void releaseExpiredUnpaidOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Order> expiredOrders = orderRepository.findUnpaidOnlineOrdersBefore(cutoff);

        if (!expiredOrders.isEmpty()) {
            log.info("Found {} expired unpaid online orders. Initiating automatic cancellation and stock release.", expiredOrders.size());
            for (Order order : expiredOrders) {
                try {
                    orderStateMachine.cancelOrder(order, UserRole.ADMIN,
                            "System auto-cancel: Payment timeout exceeded (" + timeoutMinutes + " minutes). Stock released.");
                } catch (Exception e) {
                    log.error("Failed to auto-cancel expired order {}", order.getOrderNumber(), e);
                }
            }
        }
    }
}
