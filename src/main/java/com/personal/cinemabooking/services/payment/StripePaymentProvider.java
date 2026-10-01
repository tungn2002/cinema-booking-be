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
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PdfService pdfService;
    private final EmailService emailService;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.STRIPE;
    }

    @Override
    @Transactional
    public CheckoutSessionDTO createPaymentSession(Reservation reservation, String successUrl, String cancelUrl) throws Exception {
        Stripe.apiKey = stripeApiKey;

        String description = String.format("Movie Reservation #%d - %s",
                reservation.getId(),
                reservation.getShowtime().getMovie().getTitle());

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(reservation.getId().toString())
                .setCustomerEmail(reservation.getUser().getEmail())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount((long) (reservation.getTotalPrice() * 100))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Movie Reservation")
                                        .setDescription(description)
                                        .build())
                                .build())
                        .setQuantity(1L)
                        .build())
                .build();

        Session session = Session.create(params);

        // Fix idempotency bug: Always check for existing payment, update it if exists instead of blindly overwriting intent
        Payment payment = paymentRepository.findByReservation(reservation).orElse(new Payment());
        payment.setReservation(reservation);
        payment.setPaymentIntentId(session.getId());
        payment.setAmount(reservation.getTotalPrice());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(PaymentMethod.STRIPE);
        if (payment.getId() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        } else {
            payment.setUpdatedAt(LocalDateTime.now());
        }
        
        paymentRepository.save(payment);

        return new CheckoutSessionDTO(session.getId(), session.getUrl());
    }

    @Override
    @Transactional
    public void handleWebhook(HttpServletRequest request, String payload) throws Exception {
        String sigHeader = request.getHeader("Stripe-Signature");
        Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        
        if (event.getType().equals("checkout.session.completed")) {
            JSONObject jsonObject = new JSONObject(payload);
            JSONObject data = jsonObject.getJSONObject("data");
            JSONObject object = data.getJSONObject("object");
            String sessionId = object.getString("id"); // Get session ID
            
            // Find by exact sessionId to prevent cross-contamination if user changed payment method
            Payment payment = paymentRepository.findByPaymentIntentId(sessionId).orElse(null);
            
            if (payment != null && payment.getStatus() != PaymentStatus.SUCCEEDED) {
                payment.setStatus(PaymentStatus.SUCCEEDED);
                payment.setPaymentMethod(PaymentMethod.STRIPE);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                Reservation reservation = payment.getReservation();
                reservation.setPaid(true);
                reservationRepository.save(reservation);

                // Generate PDF & Send Email
                byte[] pdfBytes = pdfService.generateReceipt(reservation, payment);
                        String emailBody = "Dear " + reservation.getUser().getFullName() + ",\n\n" +
                                "Thank you for your reservation.\n" +
                                "Movie: " + reservation.getShowtime().getMovie().getTitle() + "\n" +
                                "Time: " + reservation.getShowtime().getShowTime() + "\n" +
                                "Seats: " + reservation.getSeats().size() + "\n" +
                                "Total Paid: $" + payment.getAmount() + "\n\n" +
                                "Please find your receipt attached.";
                    emailService.sendEmailWithAttachment(
                            reservation.getUser().getEmail(),
                            "Cinema Booking - Payment Receipt #RES-" + reservation.getId(),
                            emailBody,
                            pdfBytes,
                            "receipt-" + reservation.getId() + ".pdf"
                    );
            }
        }
    }
}
