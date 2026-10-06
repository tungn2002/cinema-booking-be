package com.personal.cinemabooking.events;

import org.springframework.context.ApplicationEvent;

public class PaymentSuccessEvent extends ApplicationEvent {

    private final Long paymentId;

    public PaymentSuccessEvent(Object source, Long paymentId) {
        super(source);
        this.paymentId = paymentId;
    }

    public Long getPaymentId() {
        return paymentId;
    }
}
