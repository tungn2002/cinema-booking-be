package com.personal.cinemabooking.listeners;

import com.personal.cinemabooking.entities.Payment;
import com.personal.cinemabooking.events.PaymentSuccessEvent;
import com.personal.cinemabooking.repositories.PaymentRepository;
import com.personal.cinemabooking.services.EmailService;
import com.personal.cinemabooking.services.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSuccessEventListener {

    private final PaymentRepository paymentRepository;
    private final PdfService pdfService;
    private final EmailService emailService;

    @Async
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Processing receipt email for payment ID: {}", event.getPaymentId());
        try {
            Payment payment = paymentRepository.findById(event.getPaymentId())
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

            String pdfPath = pdfService.generateReceipt(payment);
            emailService.sendReceiptEmail(payment, pdfPath);
            log.info("Successfully sent receipt email for payment ID: {}", event.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to generate or send receipt for payment ID: {}", event.getPaymentId(), e);
            throw new RuntimeException("Failed to process payment success", e); // Wrap checked exception to satisfy compiler while allowing @Retryable to work
        }
    }
}
