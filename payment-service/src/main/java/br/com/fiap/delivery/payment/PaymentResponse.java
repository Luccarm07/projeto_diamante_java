package br.com.fiap.delivery.payment;

public record PaymentResponse(String status, int instance) {

    public static PaymentResponse approved(int instance) {
        return new PaymentResponse("APPROVED", instance);
    }
}
