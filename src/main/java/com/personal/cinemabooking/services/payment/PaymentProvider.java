package com.personal.cinemabooking.services.payment;

import com.personal.cinemabooking.dto.CheckoutSessionDTO;
import com.personal.cinemabooking.entities.Reservation;
import com.personal.cinemabooking.enums.PaymentMethod;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentProvider {
    PaymentMethod getPaymentMethod();
    
    // Instead of successUrl/cancelUrl, we just return clientSecret for Embedded UI
    CheckoutSessionDTO createPaymentSession(Reservation reservation) throws Exception;
    
    void handleWebhook(HttpServletRequest request, String payload) throws Exception;
    
    boolean checkAndSyncPayment(String paymentIntentId);
}
