package br.com.fiap.delivery.order.exception;

import org.springframework.http.HttpStatus;

public class PaymentFailedException extends ApiException {

    public PaymentFailedException() {
        super(HttpStatus.BAD_GATEWAY, "Payment service unavailable, order not confirmed");
    }

    public PaymentFailedException(Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "Payment service unavailable, order not confirmed", cause);
    }
}
