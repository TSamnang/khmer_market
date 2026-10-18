package com.example.khmer_market.web.controller;

import com.example.khmer_market.domain.entity.Address;
import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.entity.Payment;
import com.example.khmer_market.domain.enums.PaymentMethod;
import com.example.khmer_market.repository.OrderRepository;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.AddressService;
import com.example.khmer_market.service.CartService;
import com.example.khmer_market.service.CheckoutService;
import com.example.khmer_market.web.dto.CartViewDto;
import com.example.khmer_market.web.dto.CheckoutDto;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CartService cartService;
    private final AddressService addressService;
    private final OrderRepository orderRepository;
    private final com.example.khmer_market.service.ReviewService reviewService;
    private final com.example.khmer_market.repository.ReviewRepository reviewRepository;

    public CheckoutController(CheckoutService checkoutService,
                              CartService cartService,
                              AddressService addressService,
                              OrderRepository orderRepository,
                              com.example.khmer_market.service.ReviewService reviewService,
                              com.example.khmer_market.repository.ReviewRepository reviewRepository) {
        this.checkoutService = checkoutService;
        this.cartService = cartService;
        this.addressService = addressService;
        this.orderRepository = orderRepository;
        this.reviewService = reviewService;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/checkout")
    public String checkoutPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Long userId = userDetails.getUserId();
        CartViewDto cart = cartService.getCartView(userId);

        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty");
            return "redirect:/cart";
        }

        if (cart.isHasInactiveOrOutOfStockItems()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Some items in your cart are out of stock. Please adjust your cart before checkout.");
            return "redirect:/cart";
        }

        List<Address> addresses = addressService.getUserAddresses(userId);
        String token = checkoutService.generateCheckoutToken(session);

        CheckoutDto checkoutDto = new CheckoutDto();
        checkoutDto.setCheckoutToken(token);
        if (!addresses.isEmpty()) {
            checkoutDto.setAddressId(addresses.get(0).getId());
        }

        model.addAttribute("cart", cart);
        model.addAttribute("addresses", addresses);
        model.addAttribute("checkoutDto", checkoutDto);

        return "buyer/checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @Valid @ModelAttribute("checkoutDto") CheckoutDto checkoutDto,
                                  BindingResult bindingResult,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        Long userId = userDetails.getUserId();

        // Conditional address validation: if addressId is null, newAddress must be provided
        if (checkoutDto.getAddressId() == null) {
            if (checkoutDto.getNewAddress() == null ||
                checkoutDto.getNewAddress().getName() == null || checkoutDto.getNewAddress().getName().trim().isEmpty()) {
                bindingResult.rejectValue("newAddress.name", "NotBlank.newAddress.name", "Recipient name is required for new delivery address");
            }
            if (checkoutDto.getNewAddress() == null ||
                checkoutDto.getNewAddress().getPhone() == null || checkoutDto.getNewAddress().getPhone().trim().isEmpty()) {
                bindingResult.rejectValue("newAddress.phone", "NotBlank.newAddress.phone", "Phone number is required for new delivery address");
            }
        }

        if (bindingResult.hasErrors()) {
            CartViewDto cart = cartService.getCartView(userId);
            List<Address> addresses = addressService.getUserAddresses(userId);
            model.addAttribute("cart", cart);
            model.addAttribute("addresses", addresses);

            // Ensure checkout token remains available
            if (checkoutDto.getCheckoutToken() == null || checkoutDto.getCheckoutToken().isBlank()) {
                String token = checkoutService.generateCheckoutToken(session);
                checkoutDto.setCheckoutToken(token);
            }

            String firstErrorMsg = bindingResult.getAllErrors().isEmpty()
                    ? "Please check your delivery and payment details."
                    : bindingResult.getAllErrors().get(0).getDefaultMessage();
            model.addAttribute("errorMessage", firstErrorMsg);
            return "buyer/checkout";
        }

        try {
            List<Order> createdOrders = checkoutService.processCheckout(userId, checkoutDto, session);
            List<String> orderNumbers = createdOrders.stream().map(Order::getOrderNumber).toList();
            String joined = String.join(",", orderNumbers);
            return "redirect:/checkout/confirmation?orders=" + joined;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/checkout/confirmation")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String confirmationPage(@RequestParam(name = "orders") String ordersParam,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        String[] orderNos = ordersParam.split(",");
        List<Order> orders = new ArrayList<>();

        for (String orderNo : orderNos) {
            orderRepository.findByOrderNumber(orderNo.trim()).ifPresent(o -> {
                if (o.getBuyer().getId().equals(userDetails.getUserId())) {
                    if (o.getItems() != null) {
                        o.getItems().forEach(it -> {
                            if (it.getProductPackage() != null && it.getProductPackage().getProduct() != null) {
                                it.getProductPackage().getProduct().getName();
                            }
                            if (it.getSeller() != null && it.getSeller().getFarmerProfile() != null) {
                                it.getSeller().getFarmerProfile().getFarmName();
                            }
                        });
                    }
                    if (o.getPayments() != null) {
                        o.getPayments().size();
                    }
                    orders.add(o);
                }
            });
        }

        java.util.Map<Long, Boolean> itemReviewedMap = new java.util.HashMap<>();
        com.example.khmer_market.domain.entity.OrderItem firstUnreviewedItem = null;

        for (Order o : orders) {
            if (o.getItems() != null) {
                for (com.example.khmer_market.domain.entity.OrderItem it : o.getItems()) {
                    boolean reviewed = reviewRepository.existsByOrderItemId(it.getId());
                    itemReviewedMap.put(it.getId(), reviewed);
                    if (!reviewed && firstUnreviewedItem == null) {
                        firstUnreviewedItem = it;
                    }
                }
            }
        }

        model.addAttribute("orders", orders);
        model.addAttribute("ordersParam", ordersParam);
        model.addAttribute("itemReviewedMap", itemReviewedMap);
        model.addAttribute("firstUnreviewedItem", firstUnreviewedItem);
        model.addAttribute("hasUnreviewedItems", firstUnreviewedItem != null);
        if (!model.containsAttribute("reviewDto")) {
            com.example.khmer_market.web.dto.ReviewDto defaultDto = new com.example.khmer_market.web.dto.ReviewDto();
            if (firstUnreviewedItem != null) {
                defaultDto.setOrderItemId(firstUnreviewedItem.getId());
                defaultDto.setRating(5);
            }
            model.addAttribute("reviewDto", defaultDto);
        }
        return "buyer/confirmation";
    }

    @PostMapping("/checkout/review")
    public String submitConfirmationReview(@RequestParam(name = "ordersParam") String ordersParam,
                                           @Valid @ModelAttribute("reviewDto") com.example.khmer_market.web.dto.ReviewDto reviewDto,
                                           BindingResult bindingResult,
                                           @AuthenticationPrincipal CustomUserDetails userDetails,
                                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Rating between 1 and 5 stars is required.");
            return "redirect:/checkout/confirmation?orders=" + ordersParam;
        }

        try {
            reviewService.addReview(userDetails.getUserId(), reviewDto);
            redirectAttributes.addFlashAttribute("successMessage", "Thank you for your rating & review! Your feedback supports local farmers.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/checkout/confirmation?orders=" + ordersParam;
    }
}
