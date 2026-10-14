package br.com.fiap.delivery.order.exception;

import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends ApiException {

    public OrderNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Order not found");
    }

    public OrderNotFoundException(Throwable cause) {
        super(HttpStatus.NOT_FOUND, "Order not found", cause);
    }
}
