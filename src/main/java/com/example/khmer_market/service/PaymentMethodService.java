package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.entity.UserPaymentMethod;
import com.example.khmer_market.exception.ResourceNotFoundException;
import com.example.khmer_market.repository.UserPaymentMethodRepository;
import com.example.khmer_market.repository.UserRepository;
import com.example.khmer_market.web.dto.PaymentMethodDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentMethodService {

    private final UserPaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;

    public PaymentMethodService(UserPaymentMethodRepository paymentMethodRepository,
                                UserRepository userRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserPaymentMethod> getUserPaymentMethods(Long userId) {
        return paymentMethodRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public UserPaymentMethod getPaymentMethod(Long id, Long userId) {
        return paymentMethodRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found: " + id));
    }

    @Transactional
    public UserPaymentMethod savePaymentMethod(Long userId, PaymentMethodDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        long existingCount = paymentMethodRepository.countByUserId(userId);
        boolean shouldBeDefault = dto.isDefault() || existingCount == 0;

        if (shouldBeDefault) {
            paymentMethodRepository.clearDefaultMethodsForUser(userId);
        }

        UserPaymentMethod method = new UserPaymentMethod();
        method.setUser(user);
        method.setMethodType(dto.getMethodType().trim().toUpperCase());
        method.setProvider(dto.getProvider().trim());
        method.setAccountNumber(dto.getAccountNumber().trim());
        method.setAccountHolderName(dto.getAccountHolderName().trim().toUpperCase());
        method.setCardExpiry(dto.getCardExpiry() != null ? dto.getCardExpiry().trim() : null);
        method.setDefault(shouldBeDefault);

        return paymentMethodRepository.save(method);
    }

    @Transactional
    public void setDefaultPaymentMethod(Long id, Long userId) {
        UserPaymentMethod method = getPaymentMethod(id, userId);
        paymentMethodRepository.clearDefaultMethodsForUser(userId);
        method.setDefault(true);
        paymentMethodRepository.save(method);
    }

    @Transactional
    public void deletePaymentMethod(Long id, Long userId) {
        UserPaymentMethod method = getPaymentMethod(id, userId);
        boolean wasDefault = method.isDefault();
        paymentMethodRepository.delete(method);

        // If the deleted method was default, make the most recent remaining one default
        if (wasDefault) {
            List<UserPaymentMethod> remaining = paymentMethodRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                UserPaymentMethod nextDefault = remaining.get(0);
                nextDefault.setDefault(true);
                paymentMethodRepository.save(nextDefault);
            }
        }
    }
}
