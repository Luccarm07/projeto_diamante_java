package br.com.fiap.delivery.order.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(Long id, Long dishId, int quantity, BigDecimal totalPrice,
                            OrderStatus status, LocalDateTime createdAt) {

    public static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(order.getId(), order.getDishId(), order.getQuantity(),
                order.getTotalPrice(), order.getStatus(), order.getCreatedAt());
    }
}
