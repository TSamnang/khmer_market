package com.example.khmer_market.payment;

import com.example.khmer_market.domain.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentProviderFactory {

    private final Map<PaymentMethod, PaymentProvider> providers = new EnumMap<>(PaymentMethod.class);

    public PaymentProviderFactory(List<PaymentProvider> providerList) {
        for (PaymentProvider p : providerList) {
            providers.put(p.getMethod(), p);
        }
    }

    public PaymentProvider getProvider(PaymentMethod method) {
        PaymentProvider provider = providers.get(method);
        if (provider == null) {
            throw new IllegalArgumentException("No payment provider registered for: " + method);
        }
        return provider;
    }
}
