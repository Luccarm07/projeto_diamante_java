package br.com.fiap.delivery.order.exception;

import org.springframework.http.HttpStatus;

public class OutOfStockException extends ApiException {

    public OutOfStockException() {
        super(HttpStatus.CONFLICT, "Dish out of stock");
    }

    public OutOfStockException(Throwable cause) {
        super(HttpStatus.CONFLICT, "Dish out of stock", cause);
    }
}
