package br.com.fiap.delivery.order.dish;

import java.math.BigDecimal;

public record DishResponse(Long id, String name, String description, BigDecimal price, int stock) {

    public static DishResponse from(Dish dish) {
        return new DishResponse(dish.getId(), dish.getName(), dish.getDescription(),
                dish.getPrice(), dish.getStock());
    }
}
