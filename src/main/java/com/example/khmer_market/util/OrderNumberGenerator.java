package com.example.khmer_market.util;

import com.example.khmer_market.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class OrderNumberGenerator {

    private final OrderRepository orderRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public OrderNumberGenerator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public String generateUniqueOrderNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("KM-");
            sb.append(datePrefix).append("-");
            for (int i = 0; i < 6; i++) {
                int index = secureRandom.nextInt(CHARACTERS.length());
                sb.append(CHARACTERS.charAt(index));
            }
            String candidate = sb.toString();
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate unique order number after multiple attempts");
    }
}
