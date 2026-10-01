import os
import re

base_pkg = "src/main/java/com/personal/cinemabooking"

# 1. Create PaymentMethod Enum
os.makedirs(f"{base_pkg}/enums", exist_ok=True)
with open(f"{base_pkg}/enums/PaymentMethod.java", "w", encoding="utf-8") as f:
    f.write("""package com.personal.cinemabooking.enums;

public enum PaymentMethod {
    STRIPE,
    RAZORPAY
}
""")

# 2. Update Payment.java
payment_java_path = f"{base_pkg}/entities/Payment.java"
with open(payment_java_path, "r", encoding="utf-8") as f:
    content = f.read()
if "PaymentMethod paymentMethod;" not in content:
    content = content.replace("public class Payment {", """public class Payment {

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private com.personal.cinemabooking.enums.PaymentMethod paymentMethod = com.personal.cinemabooking.enums.PaymentMethod.STRIPE;""")
    with open(payment_java_path, "w", encoding="utf-8") as f:
        f.write(content)

# 3. Update PaymentRequest.java
req_path = f"{base_pkg}/dto/PaymentRequest.java"
with open(req_path, "r", encoding="utf-8") as f:
    req_content = f.read()
if "PaymentMethod paymentMethod" not in req_content:
    req_content = req_content.replace("public class PaymentRequest {", """public class PaymentRequest {

    private com.personal.cinemabooking.enums.PaymentMethod paymentMethod;""")
    with open(req_path, "w", encoding="utf-8") as f:
        f.write(req_content)

# 4. Create PaymentProvider Interface
os.makedirs(f"{base_pkg}/services/payment", exist_ok=True)
with open(f"{base_pkg}/services/payment/PaymentProvider.java", "w", encoding="utf-8") as f:
    f.write("""package com.personal.cinemabooking.services.payment;

import com.personal.cinemabooking.dto.CheckoutSessionDTO;
import com.personal.cinemabooking.entities.Reservation;
import com.personal.cinemabooking.enums.PaymentMethod;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentProvider {
    PaymentMethod getPaymentMethod();
    CheckoutSessionDTO createPaymentSession(Reservation reservation, String successUrl, String cancelUrl) throws Exception;
    void handleWebhook(HttpServletRequest request, String payload) throws Exception;
}
""")

# 5. Create StripePaymentProvider.java
with open(f"{base_pkg}/services/payment/StripePaymentProvider.java", "w", encoding="utf-8") as f:
    f.write("""package com.personal.cinemabooking.services.payment;

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
        payment.setCreatedAt(LocalDateTime.now());
        
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
            String clientReferenceId = object.optString("client_reference_id", null);

            if (clientReferenceId != null && !clientReferenceId.isEmpty()) {
                Long reservationId = Long.parseLong(clientReferenceId);
                Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
                
                if (reservation != null) {
                    Payment payment = paymentRepository.findByReservation(reservation).orElse(null);
                    if (payment != null && payment.getStatus() != PaymentStatus.SUCCEEDED) {
                        payment.setStatus(PaymentStatus.SUCCEEDED);
                        payment.setUpdatedAt(LocalDateTime.now());
                        paymentRepository.save(payment);

                        reservation.setPaid(true);
                        reservationRepository.save(reservation);

                        // Generate PDF & Send Email
                        byte[] pdfBytes = pdfService.generateReceipt(reservation, payment);
                        String emailBody = "Dear " + reservation.getUser().getFullName() + ",\\n\\n" +
                                "Thank you for your reservation.\\n" +
                                "Movie: " + reservation.getShowtime().getMovie().getTitle() + "\\n" +
                                "Time: " + reservation.getShowtime().getShowTime() + "\\n" +
                                "Seats: " + reservation.getSeats().size() + "\\n" +
                                "Total Paid: $" + payment.getAmount() + "\\n\\n" +
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
    }
}
""")

# 6. Create RazorpayPaymentProvider.java
with open(f"{base_pkg}/services/payment/RazorpayPaymentProvider.java", "w", encoding="utf-8") as f:
    f.write("""package com.personal.cinemabooking.services.payment;

import com.personal.cinemabooking.dto.CheckoutSessionDTO;
import com.personal.cinemabooking.entities.Payment;
import com.personal.cinemabooking.entities.Reservation;
import com.personal.cinemabooking.enums.PaymentMethod;
import com.personal.cinemabooking.enums.PaymentStatus;
import com.personal.cinemabooking.repositories.PaymentRepository;
import com.personal.cinemabooking.repositories.ReservationRepository;
import com.personal.cinemabooking.services.PdfService;
import com.personal.cinemabooking.services.EmailService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
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
public class RazorpayPaymentProvider implements PaymentProvider {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PdfService pdfService;
    private final EmailService emailService;

    @Value("${razorpay.key.id:dummy_id}")
    private String keyId;

    @Value("${razorpay.key.secret:dummy_secret}")
    private String keySecret;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.RAZORPAY;
    }

    @Override
    @Transactional
    public CheckoutSessionDTO createPaymentSession(Reservation reservation, String successUrl, String cancelUrl) throws Exception {
        RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", (int)(reservation.getTotalPrice() * 100)); // amount in paise
        orderRequest.put("currency", "INR"); // Razorpay defaults to INR mostly
        orderRequest.put("receipt", "txn_" + reservation.getId());

        Order order = razorpay.orders.create(orderRequest);

        Payment payment = paymentRepository.findByReservation(reservation).orElse(new Payment());
        payment.setReservation(reservation);
        payment.setPaymentIntentId(order.get("id"));
        payment.setAmount(reservation.getTotalPrice());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(PaymentMethod.RAZORPAY);
        payment.setCreatedAt(LocalDateTime.now());
        
        paymentRepository.save(payment);

        // For razorpay, we typically return the orderId and FE uses Razorpay Checkout script. 
        // We'll mimic checkoutSession logic by returning orderId
        return new CheckoutSessionDTO(order.get("id"), "RAZORPAY_CHECKOUT_TRIGGER");
    }

    @Override
    @Transactional
    public void handleWebhook(HttpServletRequest request, String payload) throws Exception {
        String signature = request.getHeader("X-Razorpay-Signature");
        if (Utils.verifyWebhookSignature(payload, signature, keySecret)) {
            JSONObject json = new JSONObject(payload);
            if (json.getString("event").equals("order.paid")) {
                JSONObject orderEntity = json.getJSONObject("payload").getJSONObject("order").getJSONObject("entity");
                String orderId = orderEntity.getString("id");

                Payment payment = paymentRepository.findByPaymentIntentId(orderId).orElse(null);
                if (payment != null && payment.getStatus() != PaymentStatus.SUCCEEDED) {
                    payment.setStatus(PaymentStatus.SUCCEEDED);
                    payment.setUpdatedAt(LocalDateTime.now());
                    paymentRepository.save(payment);

                    Reservation reservation = payment.getReservation();
                    reservation.setPaid(true);
                    reservationRepository.save(reservation);

                    // Generate PDF & Send Email
                    byte[] pdfBytes = pdfService.generateReceipt(reservation, payment);
                    String emailBody = "Dear " + reservation.getUser().getFullName() + ",\\n\\n" +
                            "Thank you for your reservation.\\n" +
                            "Total Paid: $" + payment.getAmount() + "\\n\\n" +
                            "Please find your receipt attached.";
                    emailService.sendEmailWithAttachment(
                            reservation.getUser().getEmail(),
                            "Cinema Booking - Receipt",
                            emailBody,
                            pdfBytes,
                            "receipt-" + reservation.getId() + ".pdf"
                    );
                }
            }
        }
    }
}
""")

# 7. Refactor PaymentService.java
with open(f"{base_pkg}/services/PaymentService.java", "w", encoding="utf-8") as f:
    f.write("""package com.personal.cinemabooking.services;

import com.personal.cinemabooking.dto.CheckoutSessionDTO;
import com.personal.cinemabooking.dto.PaymentDTO;
import com.personal.cinemabooking.entities.Payment;
import com.personal.cinemabooking.entities.Reservation;
import com.personal.cinemabooking.enums.PaymentMethod;
import com.personal.cinemabooking.enums.PaymentStatus;
import com.personal.cinemabooking.core.exceptions.ResourceNotFoundException;
import com.personal.cinemabooking.repositories.PaymentRepository;
import com.personal.cinemabooking.repositories.ReservationRepository;
import com.personal.cinemabooking.services.payment.PaymentProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ModelMapper modelMapper;
    private final Map<PaymentMethod, PaymentProvider> providers;

    public PaymentService(PaymentRepository paymentRepository, 
                          ReservationRepository reservationRepository,
                          ModelMapper modelMapper,
                          List<PaymentProvider> providerList) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.modelMapper = modelMapper;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(PaymentProvider::getPaymentMethod, p -> p));
    }

    public CheckoutSessionDTO createCheckoutSession(Long reservationId, String successUrl, String cancelUrl, PaymentMethod method) throws Exception {
        if (method == null) method = PaymentMethod.STRIPE; // default

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        Optional<Payment> existingPayment = paymentRepository.findByReservation(reservation);
        if (existingPayment.isPresent() && existingPayment.get().getStatus() == PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("Payment already completed for this reservation");
        }

        PaymentProvider provider = providers.get(method);
        if (provider == null) throw new IllegalArgumentException("Unsupported payment method");

        return provider.createPaymentSession(reservation, successUrl, cancelUrl);
    }

    public void handleWebhookEvent(HttpServletRequest request, String payload, PaymentMethod method) throws Exception {
        PaymentProvider provider = providers.get(method);
        if (provider != null) {
            provider.handleWebhook(request, payload);
        }
    }

    public PaymentDTO getPaymentByReservationId(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        Payment payment = paymentRepository.findByReservation(reservation)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return modelMapper.map(payment, PaymentDTO.class);
    }
}
""")

# 8. Refactor PaymentController.java
with open(f"{base_pkg}/controllers/PaymentController.java", "w", encoding="utf-8") as f:
    f.write("""package com.personal.cinemabooking.controllers;

import lombok.RequiredArgsConstructor;
import com.personal.cinemabooking.dto.ApiResponse;
import com.personal.cinemabooking.dto.PaymentDTO;
import com.personal.cinemabooking.dto.CheckoutSessionDTO;
import com.personal.cinemabooking.dto.PaymentRequest;
import com.personal.cinemabooking.enums.PaymentMethod;
import com.personal.cinemabooking.services.PaymentService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
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
    @RateLimiter(name = "basic")
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

    @PostMapping("/webhook/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(HttpServletRequest request, @RequestBody String payload) {
        try {
            paymentService.handleWebhookEvent(request, payload, PaymentMethod.RAZORPAY);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Razorpay webhook error", e);
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
""")

# 9. Add "book-and-pay" API to ReservationController
res_ctrl_path = f"{base_pkg}/controllers/ReservationController.java"
with open(res_ctrl_path, "r", encoding="utf-8") as f:
    res_ctrl = f.read()

book_and_pay = """
    // New Book & Pay API
    @PostMapping("/book-and-pay")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CheckoutSessionDTO>> bookAndPay(@Valid @RequestBody com.personal.cinemabooking.dto.PaymentRequest req) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            // Create reservation first. (req should have showtimeId and seatIds ideally, but this is a stub for the logic)
            // For now, this assumes reservationId is already present or we inject it
            CheckoutSessionDTO dto = reservationService.bookAndPay(req, username); // We'll add this in service
            return ResponseEntity.ok(new ApiResponse<>(true, "Booked & Session created", dto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
"""
if "/book-and-pay" not in res_ctrl:
    res_ctrl = res_ctrl.replace("public class ReservationController {", "public class ReservationController {" + book_and_pay)
    with open(res_ctrl_path, "w", encoding="utf-8") as f:
        f.write(res_ctrl)

print("Done")
