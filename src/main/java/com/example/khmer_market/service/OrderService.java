package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.entity.OrderItem;
import com.example.khmer_market.domain.enums.OrderStatus;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.exception.ResourceNotFoundException;
import com.example.khmer_market.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStateMachine orderStateMachine;

    public OrderService(OrderRepository orderRepository,
                        OrderStateMachine orderStateMachine) {
        this.orderRepository = orderRepository;
        this.orderStateMachine = orderStateMachine;
    }

    @Transactional(readOnly = true)
    public Page<Order> getBuyerOrders(Long buyerId, OrderStatus status, Pageable pageable) {
        if (status != null) {
            return orderRepository.findByBuyerIdAndStatus(buyerId, status, pageable);
        }
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable);
    }

    @Transactional(readOnly = true)
    public Order getBuyerOrderDetail(Long orderId, Long buyerId) {
        return orderRepository.findByIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or access denied"));
    }

    @Transactional(readOnly = true)
    public Page<Order> getFarmerOrders(Long sellerId, OrderStatus status, Pageable pageable) {
        return orderRepository.findBySellerId(sellerId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Order getFarmerOrderDetail(Long orderId, Long sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        boolean belongsToSeller = order.getItems().stream()
                .anyMatch(item -> item.getSeller().getId().equals(sellerId));

        if (!belongsToSeller) {
            throw new ResourceNotFoundException("Order not found or access denied");
        }

        return order;
    }

    @Transactional
    public void confirmOrderAsFarmer(Long orderId, Long sellerId) {
        Order order = getFarmerOrderDetail(orderId, sellerId);
        orderStateMachine.confirmOrder(order, UserRole.FARMER);
    }

    @Transactional
    public void shipOrderAsFarmer(Long orderId, Long sellerId) {
        Order order = getFarmerOrderDetail(orderId, sellerId);
        orderStateMachine.shipOrder(order, UserRole.FARMER);
    }

    @Transactional
    public void deliverOrderAsFarmer(Long orderId, Long sellerId) {
        Order order = getFarmerOrderDetail(orderId, sellerId);
        orderStateMachine.deliverOrder(order, UserRole.FARMER);
    }

    @Transactional
    public void confirmReceiptAsBuyer(Long orderId, Long buyerId) {
        Order order = getBuyerOrderDetail(orderId, buyerId);
        orderStateMachine.deliverOrder(order, UserRole.BUYER);
    }

    @Transactional
    public void cancelOrderAsBuyer(Long orderId, Long buyerId, String reason) {
        Order order = getBuyerOrderDetail(orderId, buyerId);
        orderStateMachine.cancelOrder(order, UserRole.BUYER, reason != null ? reason : "Cancelled by buyer");
    }

    @Transactional
    public void cancelOrderAsFarmer(Long orderId, Long sellerId, String reason) {
        Order order = getFarmerOrderDetail(orderId, sellerId);
        orderStateMachine.cancelOrder(order, UserRole.FARMER, reason != null ? reason : "Cancelled by farmer");
    }

    @Transactional
    public void cancelOrderAsAdmin(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        orderStateMachine.cancelOrder(order, UserRole.ADMIN, reason != null ? reason : "Cancelled by platform administrator");
    }
}
