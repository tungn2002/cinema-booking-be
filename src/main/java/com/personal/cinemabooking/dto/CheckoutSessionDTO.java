package com.personal.cinemabooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutSessionDTO {
    private String paymentIntentId;
    private String clientSecret; // Replaces URL for embedded UI
}
