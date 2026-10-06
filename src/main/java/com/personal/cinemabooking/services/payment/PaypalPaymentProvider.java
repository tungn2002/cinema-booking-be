package com.personal.cinemabooking.services.payment;

import com.personal.cinemabooking.dto.CheckoutSessionDTO;
import com.personal.cinemabooking.entities.Payment;
import com.personal.cinemabooking.entities.Reservation;
import com.personal.cinemabooking.enums.PaymentMethod;
import com.personal.cinemabooking.enums.PaymentStatus;
import com.personal.cinemabooking.repositories.PaymentRepository;
import com.personal.cinemabooking.repositories.ReservationRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Base64;
import com.personal.cinemabooking.services.ReservationService;
import com.personal.cinemabooking.events.PaymentSuccessEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaypalPaymentProvider implements PaymentProvider {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ApplicationContext applicationContext;
    private final RestClient restClient = RestClient.create();

    @Value("${paypal.client.id:dummy}")
    private String clientId;

    @Value("${paypal.client.secret:dummy}")
    private String clientSecret;

    @Value("${paypal.mode:sandbox}")
    private String mode;

    @Value("${reservation.expiration.minutes:10}")
    private int expirationMinutes;

    private String getBaseUrl() {
        return "sandbox".equalsIgnoreCase(mode) ? "https://api-m.sandbox.paypal.com" : "https://api-m.paypal.com";
    }

    private String getAccessToken() {
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        ResponseEntity<String> response = restClient.post()
                .uri(getBaseUrl() + "/v1/oauth2/token")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        JSONObject json = new JSONObject(response.getBody());
        return json.getString("access_token");
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.PAYPAL;
    }

    @Override
    @Transactional
    public CheckoutSessionDTO createPaymentSession(Reservation reservation) throws Exception {
        String accessToken = getAccessToken();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("intent", "CAPTURE");

        JSONObject amount = new JSONObject();
        amount.put("currency_code", "USD");
        amount.put("value", String.format("%.2f", reservation.getTotalPrice()));

        JSONObject purchaseUnit = new JSONObject();
        purchaseUnit.put("reference_id", reservation.getId().toString());
        purchaseUnit.put("amount", amount);

        orderRequest.put("purchase_units", new JSONArray().put(purchaseUnit));

        ResponseEntity<String> response = restClient.post()
                .uri(getBaseUrl() + "/v2/checkout/orders")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(orderRequest.toString())
                .retrieve()
                .toEntity(String.class);

        JSONObject jsonResponse = new JSONObject(response.getBody());
        String orderId = jsonResponse.getString("id");

        Payment payment = paymentRepository.findByReservation(reservation).orElse(new Payment());
        payment.setReservation(reservation);
        payment.setPaymentIntentId(orderId);
        payment.setAmount(reservation.getTotalPrice());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(PaymentMethod.PAYPAL);
        
        if (payment.getId() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        } else {
            payment.setUpdatedAt(LocalDateTime.now());
        }
        paymentRepository.save(payment);

        return new CheckoutSessionDTO(orderId, orderId); // clientSecret is just the orderId for Paypal
    }

    @Override
    @Transactional
    public void handleWebhook(HttpServletRequest request, String payload) throws Exception {
        log.info("Received PayPal Webhook: {}", payload);
        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event_type");

        if ("CHECKOUT.ORDER.APPROVED".equals(eventType)) {
            JSONObject resource = event.getJSONObject("resource");
            String orderId = resource.getString("id");
            
            Payment payment = paymentRepository.findByPaymentIntentId(orderId).orElse(null);
            if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
                Reservation reservation = payment.getReservation();

                LocalDateTime expirationTime = reservation.getReservationTime().plusMinutes(expirationMinutes);
                boolean isExpired = LocalDateTime.now().isAfter(expirationTime);

                if (reservation.getStatusId() == 3 || isExpired) {
                    log.warn("Reservation {} cancelled or expired. Voiding Paypal Order {}.", reservation.getId(), orderId);
                    payment.setStatus(PaymentStatus.FAILED);
                    
                    if (reservation.getStatusId() != 3) {
                        ReservationService reservationService = 
                            applicationContext.getBean(ReservationService.class);
                        reservationService.cancelReservationSystem(reservation.getId());
                    }
                } else {
                    // Valid! Capture the order.
                    String accessToken = getAccessToken();
                    
                    try {
                        ResponseEntity<String> captureRes = restClient.post()
                            .uri(getBaseUrl() + "/v2/checkout/orders/" + orderId + "/capture")
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .body("{}")
                            .retrieve()
                            .toEntity(String.class);
                            
                        if (captureRes.getStatusCode() == HttpStatus.CREATED || captureRes.getStatusCode() == HttpStatus.OK) {
                            log.info("Successfully captured Paypal Order {}", orderId);
                            payment.setStatus(PaymentStatus.SUCCEEDED);
                            reservation.setPaid(true);
                            reservation.setStatusId(1);
                            
                            reservationRepository.save(reservation);
                            
                            applicationContext.publishEvent(new PaymentSuccessEvent(this, payment.getId()));
                        }
                    } catch (Exception e) {
                        log.error("Failed to capture Paypal order {}", orderId, e);
                    }
                }
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
        }
    }

    @Override
    public boolean checkAndSyncPayment(String paymentIntentId) {
        try {
            Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId).orElse(null);
            if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
                return payment != null && payment.getStatus() == PaymentStatus.SUCCEEDED;
            }

            Reservation reservation = payment.getReservation();

            // Attempt to capture
            String accessToken = getAccessToken();
            
            ResponseEntity<String> captureRes = restClient.post()
                .uri(getBaseUrl() + "/v2/checkout/orders/" + paymentIntentId + "/capture")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body("{}")
                .retrieve()
                .toEntity(String.class);
                
            if (captureRes.getStatusCode() == HttpStatus.CREATED || captureRes.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully captured Paypal Order via sync {}", paymentIntentId);
                payment.setStatus(PaymentStatus.SUCCEEDED);
                reservation.setPaid(true);
                reservation.setStatusId(1);
                
                reservationRepository.save(reservation);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                
                applicationContext.publishEvent(new PaymentSuccessEvent(this, payment.getId()));
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to sync/capture Paypal order {}", paymentIntentId, e);
        }
        return false;
    }
}
