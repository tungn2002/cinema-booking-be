package com.personal.cinemabooking.enums;

public enum PaymentStatus {
    PENDING,  // initial state
    SUCCEEDED, // payment completed successfully
    FAILED,   // payment failed
    REFUNDED  // payment was refunded
}
