package com.personal.cinemabooking.service;

import com.itextpdf.text.DocumentException;
import com.personal.cinemabooking.dto.CheckoutSessionDTO;
import com.personal.cinemabooking.entity.Payment;
import com.personal.cinemabooking.entity.Reservation;
import com.personal.cinemabooking.entity.User;
import com.personal.cinemabooking.entity.Showtime;
import com.personal.cinemabooking.entity.Movie;
import com.personal.cinemabooking.repo.PaymentRepository;
import com.personal.cinemabooking.repo.ReservationRepository;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PdfService pdfService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "whsec_sample");
    }

    @Test
    void testHandleCheckoutSessionCompleted() throws Exception, IOException, DocumentException, MessagingException {
        // This is a simplified test since we can't easily mock Stripe objects
        // In a real test, you would use a more comprehensive approach

        // Setup
        Payment payment = new Payment();
        payment.setStatus(Payment.PaymentStatus.PENDING);
        Reservation reservation = new Reservation();
        payment.setReservation(reservation);

        when(paymentRepository.findByPaymentIntentId(anyString())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Create a mock JSON payload that mimics Stripe's checkout.session.completed event
        String mockPayload = """
            {
                "id": "evt_test",
                "type": "checkout.session.completed",
                "data": {
                    "object": {
                        "id": "cs_test_123",
                        "client_reference_id": "1",
                        "payment_intent": "pi_test_123"
                    }
                }
            }
            """;

        // Test a private method using reflection - pass String payload, not Session object
        ReflectionTestUtils.invokeMethod(
            paymentService,
            "handleCheckoutSessionCompleted",
            mockPayload
        );

        // Verify
        verify(paymentRepository).save(any(Payment.class));
        assertEquals(Payment.PaymentStatus.SUCCEEDED, payment.getStatus());
    }

    @Test
    void testGetPaymentByReservationIdThrowsExceptionWhenNotFound() {
        // Setup
        when(reservationRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Test & Verify
        assertThrows(Exception.class, () -> {
            paymentService.getPaymentByReservationId(1L);
        });
    }
    
    @Test
    void testGenerateAndSendReceipt() throws DocumentException, MessagingException, IOException {
        // Setup
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(Payment.PaymentStatus.SUCCEEDED);

        Reservation reservation = new Reservation();
        reservation.setId(1L);

        User user = new User();
        user.setUserName("testuser");
        user.setEmail("test@example.com");
        reservation.setUser(user);

        Showtime showtime = new Showtime();
        Movie movie = new Movie();
        movie.setTitle("Test Movie");
        showtime.setMovie(movie);
        reservation.setShowtime(showtime);

        payment.setReservation(reservation);

        // Mock PDF service
        when(pdfService.generateReceipt(any(Payment.class))).thenReturn("test-path/receipt.pdf");

        // Test
        paymentService.generateAndSendReceipt(payment);

        // Verify
        verify(pdfService).generateReceipt(payment);
        verify(emailService).sendReceiptEmail(eq(payment), anyString());
        verify(paymentRepository).save(payment);
    }
}
