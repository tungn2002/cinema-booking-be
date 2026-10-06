package com.personal.cinemabooking.services.payment;

import com.personal.cinemabooking.dto.CheckoutSessionDTO;
import com.personal.cinemabooking.entities.Payment;
import com.personal.cinemabooking.entities.Reservation;
import com.personal.cinemabooking.enums.PaymentMethod;
import com.personal.cinemabooking.enums.PaymentStatus;
import com.personal.cinemabooking.repositories.PaymentRepository;
import com.personal.cinemabooking.repositories.ReservationRepository;
import com.personal.cinemabooking.services.PdfService;
import com.personal.cinemabooking.services.EmailService;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.springframework.context.ApplicationContext;
import com.personal.cinemabooking.services.ReservationService;
import com.personal.cinemabooking.events.PaymentSuccessEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final ApplicationContext applicationContext;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${reservation.expiration.minutes:10}")
    private int expirationMinutes;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.STRIPE;
    }

    @Override
    @Transactional
    public CheckoutSessionDTO createPaymentSession(Reservation reservation) throws Exception {
        Stripe.apiKey = stripeApiKey;

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (reservation.getTotalPrice() * 100))
                .setCurrency("usd")
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL) // Hold funds, no capture yet!
                .putMetadata("reservationId", reservation.getId().toString())
                .setReceiptEmail(reservation.getUser().getEmail())
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        Payment payment = paymentRepository.findByReservation(reservation).orElse(new Payment());
        payment.setReservation(reservation);
        payment.setPaymentIntentId(intent.getId());
        payment.setAmount(reservation.getTotalPrice());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(PaymentMethod.STRIPE);
        if (payment.getId() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        } else {
            payment.setUpdatedAt(LocalDateTime.now());
        }
        
        paymentRepository.save(payment);

        return new CheckoutSessionDTO(intent.getId(), intent.getClientSecret());
    }

    @Override
    @Transactional
    public void handleWebhook(HttpServletRequest request, String payload) throws Exception {
        Stripe.apiKey = stripeApiKey;
        String sigHeader = request.getHeader("Stripe-Signature");
        Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        
        if (event.getType().equals("payment_intent.amount_capturable_updated")) {
            JSONObject jsonObject = new JSONObject(payload);
            String intentId = jsonObject.getJSONObject("data").getJSONObject("object").getString("id");
            
            Payment payment = paymentRepository.findByPaymentIntentId(intentId).orElse(null);
            
            if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
                Reservation reservation = payment.getReservation();
                PaymentIntent intent = PaymentIntent.retrieve(intentId);

                LocalDateTime expirationTime = reservation.getReservationTime().plusMinutes(expirationMinutes);
                boolean isExpired = LocalDateTime.now().isAfter(expirationTime);

                if (reservation.getStatusId() == 3 || isExpired) {
                    // Over 10 mins or already cancelled! We MUST VOID the transaction.
                    log.warn("Reservation {} cancelled or expired (isExpired: {}). Voiding Stripe PaymentIntent {}.", 
                             reservation.getId(), isExpired, intentId);
                    intent.cancel();
                    payment.setStatus(PaymentStatus.FAILED);
                    
                    if (reservation.getStatusId() != 3) {
                        ReservationService reservationService = 
                            applicationContext.getBean(ReservationService.class);
                        reservationService.cancelReservationSystem(reservation.getId());
                    }
                } else {
                    // Valid! Capture the funds.
                    log.info("Reservation {} valid. Capturing Stripe PaymentIntent {}.", reservation.getId(), intentId);
                    intent.capture();
                    
                    payment.setStatus(PaymentStatus.SUCCEEDED);
                    reservation.setPaid(true);
                    reservation.setStatusId(1); // Assume confirmed
                    
                    reservationRepository.save(reservation);
                    
                    // Publish event to generate PDF and send email async after commit
                    applicationContext.publishEvent(new PaymentSuccessEvent(this, payment.getId()));
                }
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
        }
    }

    @Override
    public boolean checkAndSyncPayment(String paymentIntentId) {
        try {
            Stripe.apiKey = stripeApiKey;
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            // If the status is succeeded, it means the user paid and the funds were captured.
            // If it's requires_capture, the user authorized the payment but we haven't captured it yet.
            // If they paid, but we missed the webhook, we should capture it here to be safe and return true.
            if ("succeeded".equals(intent.getStatus())) {
                return true;
            } else if ("requires_capture".equals(intent.getStatus())) {
                intent.capture();
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to check Stripe payment intent {}", paymentIntentId, e);
        }
        return false;
    }
}
