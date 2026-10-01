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
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaypalPaymentProvider implements PaymentProvider {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PdfService pdfService;
    private final EmailService emailService;

    @Value("${paypal.client.id:dummy}")
    private String clientId;

    @Value("${paypal.client.secret:dummy}")
    private String clientSecret;

    @Value("${paypal.mode:sandbox}")
    private String mode;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.PAYPAL;
    }

    @Override
    @Transactional
    public CheckoutSessionDTO createPaymentSession(Reservation reservation, String successUrl, String cancelUrl) throws Exception {
        APIContext apiContext = new APIContext(clientId, clientSecret, mode);

        // Set payment details
        Details details = new Details();
        details.setShipping("0");
        details.setSubtotal(String.format("%.2f", reservation.getTotalPrice()));
        details.setTax("0");

        Amount amount = new Amount();
        amount.setCurrency("USD");
        amount.setTotal(String.format("%.2f", reservation.getTotalPrice()));
        amount.setDetails(details);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription("Movie Reservation #" + reservation.getId());
        transaction.setCustom(reservation.getId().toString()); // store reservation ID in custom field

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        com.paypal.api.payments.Payment paypalPayment = new com.paypal.api.payments.Payment();
        paypalPayment.setIntent("sale");
        paypalPayment.setPayer(payer);
        paypalPayment.setTransactions(transactions);

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);
        paypalPayment.setRedirectUrls(redirectUrls);

        // Create payment
        com.paypal.api.payments.Payment createdPayment = paypalPayment.create(apiContext);

        // Find approval URL
        String approvalUrl = null;
        for (Links link : createdPayment.getLinks()) {
            if (link.getRel().equals("approval_url")) {
                approvalUrl = link.getHref();
                break;
            }
        }

        if (approvalUrl == null) {
            throw new Exception("Could not get approval URL from PayPal");
        }

        // Save to our DB
        Payment payment = paymentRepository.findByReservation(reservation).orElse(new Payment());
        payment.setReservation(reservation);
        payment.setPaymentIntentId(createdPayment.getId()); // Store PayPal Payment ID
        payment.setAmount(reservation.getTotalPrice());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(PaymentMethod.PAYPAL);
        
        if (payment.getId() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        } else {
            payment.setUpdatedAt(LocalDateTime.now());
        }
        
        paymentRepository.save(payment);

        return new CheckoutSessionDTO(createdPayment.getId(), approvalUrl);
    }

    @Override
    @Transactional
    public void handleWebhook(HttpServletRequest request, String payload) throws Exception {
        // PayPal Webhook processing
        // For simplicity, we just parse the event manually or FE calls an execution endpoint
        // A complete PayPal integration usually requires FE to call 'execute' after redirect,
        // or handling 'PAYMENT.SALE.COMPLETED' webhook.
        log.info("Received PayPal Webhook: {}", payload);
        // Add full signature validation logic here in production using PayPal-Transmission-Id etc.
    }
}
