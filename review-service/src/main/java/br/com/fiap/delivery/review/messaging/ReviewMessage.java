package br.com.fiap.delivery.review.messaging;

/** JSON received from reviews.queue: {"dishId":1,"dishName":"...","rating":5,"comment":"..."} */
public record ReviewMessage(Long dishId, String dishName, int rating, String comment) {

    public boolean isValid() {
        return dishId != null && rating >= 1 && rating <= 5;
    }
}
