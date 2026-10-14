package br.com.fiap.delivery.order.payment;

/** Thrown when payment-service answers 2xx but without an APPROVED status. */
public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException(String message) {
        super(message);
    }
}
