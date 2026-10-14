package br.com.fiap.delivery.order.payment;

public record PaymentResponse(String status, int instance) {

    public boolean isApproved() {
        return "APPROVED".equals(status);
    }
}
