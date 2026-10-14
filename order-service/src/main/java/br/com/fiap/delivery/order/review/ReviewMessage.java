package br.com.fiap.delivery.order.review;

/** JSON payload published to delivery.exchange with routing key reviews.new */
public record ReviewMessage(Long dishId, String dishName, int rating, String comment) {
}
