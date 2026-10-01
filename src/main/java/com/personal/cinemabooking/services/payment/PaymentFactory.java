package com.personal.cinemabooking.services.payment;

import com.personal.cinemabooking.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PaymentFactory {
    private final Map<PaymentMethod, PaymentProvider> providers;

    public PaymentFactory(List<PaymentProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(PaymentProvider::getPaymentMethod, p -> p));
    }

    public PaymentProvider getProvider(PaymentMethod method) {
        if (method == null) {
            method = PaymentMethod.STRIPE; // default fallback
        }
        PaymentProvider provider = providers.get(method);
        if (provider == null) {
            throw new IllegalArgumentException("Không hỗ trợ phương thức thanh toán này: " + method);
        }
        return provider;
    }
}
