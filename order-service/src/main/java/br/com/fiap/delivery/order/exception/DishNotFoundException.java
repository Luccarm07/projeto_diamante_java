package br.com.fiap.delivery.order.exception;

import org.springframework.http.HttpStatus;

public class DishNotFoundException extends ApiException {

    public DishNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Dish not found");
    }

    public DishNotFoundException(Throwable cause) {
        super(HttpStatus.NOT_FOUND, "Dish not found", cause);
    }
}
