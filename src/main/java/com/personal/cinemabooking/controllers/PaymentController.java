package com.personal.cinemabooking.controllers;

import lombok.RequiredArgsConstructor;
import com.personal.cinemabooking.dto.ApiResponse;
import com.personal.cinemabooking.dto.PaymentDTO;
import com.personal.cinemabooking.dto.CheckoutSessionDTO;
import com.personal.cinemabooking.dto.PaymentRequest;
import com.personal.cinemabooking.enums.PaymentMethod;
import com.personal.cinemabooking.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final MessageSource messageSource;

    @PostMapping("/create-checkout-session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CheckoutSessionDTO>> createCheckoutSession(@Valid @RequestBody PaymentRequest req) {
        try {
            if (req.getSuccessUrl() == null || req.getCancelUrl() == null) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "URLs required", null));
            }
            CheckoutSessionDTO dto = paymentService.createCheckoutSession(req.getReservationId(), req.getSuccessUrl(), req.getCancelUrl(), req.getPaymentMethod());
            return ResponseEntity.ok(new ApiResponse<>(true, "Session created", dto));
        } catch (Exception e) {
            log.error("Error creating session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request, @RequestBody String payload) {
        try {
            paymentService.handleWebhookEvent(request, payload, PaymentMethod.STRIPE);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Stripe webhook error", e);
            return ResponseEntity.badRequest().body("Error");
        }
    }

    @PostMapping("/webhook/paypal")
    public ResponseEntity<String> handlePaypalWebhook(HttpServletRequest request, @RequestBody String payload) {
        try {
            paymentService.handleWebhookEvent(request, payload, PaymentMethod.PAYPAL);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Paypal webhook error", e);
            return ResponseEntity.badRequest().body("Error");
        }
    }

    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPayment(@PathVariable Long reservationId) {
        try {
            return ResponseEntity.ok(new ApiResponse<>(true, "Retrieved", paymentService.getPaymentByReservationId(reservationId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
