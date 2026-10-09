package com.personal.cinemabooking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.personal.cinemabooking.enums.PaymentMethod;

// payment request for stripe checkout
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    private PaymentMethod paymentMethod;
    @NotNull(message = "Reservation ID is required")
    private Long reservationId;  // which reservation to pay for

    // TODO: maybe add payment method type later?
}
