package br.com.fiap.delivery.order.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull(message = "dishId is required")
        Long dishId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity) {
}
