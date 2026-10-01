package com.personal.cinemabooking.services;

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

import java.util.Optional;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ModelMapper modelMapper;
    private final PaymentFactory paymentFactory;

    public PaymentService(PaymentRepository paymentRepository, 
                          ReservationRepository reservationRepository,
                          ModelMapper modelMapper,
                          PaymentFactory paymentFactory) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.modelMapper = modelMapper;
        this.paymentFactory = paymentFactory;
    }

    public CheckoutSessionDTO createCheckoutSession(Long reservationId, String successUrl, String cancelUrl, PaymentMethod method) throws Exception {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        Optional<Payment> existingPayment = paymentRepository.findByReservation(reservation);
        if (existingPayment.isPresent() && existingPayment.get().getStatus() == PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("Payment already completed for this reservation");
        }

        PaymentProvider provider = paymentFactory.getProvider(method);
        return provider.createPaymentSession(reservation, successUrl, cancelUrl);
    }

    public void handleWebhookEvent(HttpServletRequest request, String payload, PaymentMethod method) throws Exception {
        PaymentProvider provider = paymentFactory.getProvider(method);
        provider.handleWebhook(request, payload);
    }

    public PaymentDTO getPaymentByReservationId(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        Payment payment = paymentRepository.findByReservation(reservation)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return modelMapper.map(payment, PaymentDTO.class);
    }
}
