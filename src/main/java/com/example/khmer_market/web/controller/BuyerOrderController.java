package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.enums.OrderStatus;
import com.example.khmer_market.repository.ReviewRepository;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.OrderService;
import com.example.khmer_market.service.ReviewService;
import com.example.khmer_market.web.dto.ReviewDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/buyer/orders")
public class BuyerOrderController {

    private final OrderService orderService;
    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;

    public BuyerOrderController(OrderService orderService,
                                ReviewService reviewService,
                                ReviewRepository reviewRepository) {
        this.orderService = orderService;
        this.reviewService = reviewService;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping
    public String listOrders(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model) {
        Long userId = userDetails.getUserId();
        Pageable pageable = PageRequest.of(Math.max(0, page), size);
        Page<Order> orders;

        boolean isToReview = "to_review".equalsIgnoreCase(status);
        if (isToReview) {
            orders = orderService.getBuyerUnreviewedOrders(userId, pageable);
        } else {
            OrderStatus orderStatus = null;
            if (status != null && !status.isBlank()) {
                try {
                    orderStatus = OrderStatus.valueOf(status.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }
            orders = orderService.getBuyerOrders(userId, orderStatus, pageable);
        }

        // Determine if each delivered order has unreviewed items
        Map<Long, Boolean> orderHasUnreviewedMap = new HashMap<>();
        for (Order o : orders.getContent()) {
            if (o.getStatus() == OrderStatus.DELIVERED && o.getItems() != null && !o.getItems().isEmpty()) {
                boolean hasUnreviewed = o.getItems().stream()
                        .anyMatch(item -> !reviewRepository.existsByOrderItemId(item.getId()));
                orderHasUnreviewedMap.put(o.getId(), hasUnreviewed);
            } else {
                orderHasUnreviewedMap.put(o.getId(), false);
            }
        }

        long toReviewCount = orderService.countBuyerUnreviewedOrders(userId);

        model.addAttribute("orders", orders);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("isToReview", isToReview);
        model.addAttribute("toReviewCount", toReviewCount);
        model.addAttribute("orderHasUnreviewedMap", orderHasUnreviewedMap);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orders.getTotalPages());

        return "buyer/orders";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id,
                              @RequestParam(required = false) String action,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model) {
        Order order = orderService.getBuyerOrderDetail(id, userDetails.getUserId());

        // Check which items have already been reviewed
        Map<Long, Boolean> itemReviewedMap = new HashMap<>();
        boolean hasUnreviewed = false;
        if (order.getItems() != null) {
            for (com.example.khmer_market.domain.entity.OrderItem item : order.getItems()) {
                boolean reviewed = reviewRepository.existsByOrderItemId(item.getId());
                itemReviewedMap.put(item.getId(), reviewed);
                if (!reviewed) {
                    hasUnreviewed = true;
                }
            }
        }

        model.addAttribute("order", order);
        model.addAttribute("itemReviewedMap", itemReviewedMap);
        model.addAttribute("hasUnreviewedItems", hasUnreviewed);
        model.addAttribute("reviewDto", new ReviewDto());

        if ("review".equalsIgnoreCase(action) && hasUnreviewed) {
            model.addAttribute("autoOpenReview", true);
        }

        return "buyer/order-detail";
    }

    @PostMapping("/{id}/confirm-delivery")
    public String confirmDelivery(@PathVariable Long id,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            orderService.confirmReceiptAsBuyer(id, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Delivery confirmed! Thank you for supporting our farmers.");
            redirectAttributes.addFlashAttribute("autoOpenReview", true);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/buyer/orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrderAsBuyer(id, userDetails.getUserId(), "Cancelled by buyer");
            redirectAttributes.addFlashAttribute("successMessage", "Order has been cancelled and stock released.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/buyer/orders/" + id;
    }

    @PostMapping("/{id}/review")
    public String submitReview(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               @Valid @ModelAttribute("reviewDto") ReviewDto reviewDto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Rating between 1 and 5 is required.");
            return "redirect:/buyer/orders/" + id;
        }

        try {
            reviewService.addReview(userDetails.getUserId(), reviewDto);

            // Check if there are remaining unreviewed items in this order
            Order order = orderService.getBuyerOrderDetail(id, userDetails.getUserId());
            boolean remainingUnreviewed = order.getItems() != null && order.getItems().stream()
                    .anyMatch(item -> !reviewRepository.existsByOrderItemId(item.getId()));

            if (remainingUnreviewed) {
                redirectAttributes.addFlashAttribute("successMessage", "Review submitted! Please rate the remaining produce in this order.");
                redirectAttributes.addFlashAttribute("autoOpenReview", true);
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Thank you! All items in this order have been reviewed.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/buyer/orders/" + id;
    }
}
