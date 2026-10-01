package com.personal.cinemabooking.services.payment;

import com.personal.cinemabooking.dto.CheckoutSessionDTO;
import com.personal.cinemabooking.entities.Reservation;
import com.personal.cinemabooking.enums.PaymentMethod;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentProvider {
    PaymentMethod getPaymentMethod();
    CheckoutSessionDTO createPaymentSession(Reservation reservation, String successUrl, String cancelUrl) throws Exception;
    void handleWebhook(HttpServletRequest request, String payload) throws Exception;
}
