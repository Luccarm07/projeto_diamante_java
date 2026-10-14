package br.com.fiap.delivery.review.buffer;

/** Immutable partial aggregate of one dish: sum of ratings and number of reviews. */
public record PendingReviews(String dishName, long ratingSum, long count) {

    public static PendingReviews of(String dishName, int rating) {
        return new PendingReviews(dishName, rating, 1);
    }

    public PendingReviews plus(PendingReviews other) {
        String name = other.dishName() != null ? other.dishName() : dishName;
        return new PendingReviews(name, ratingSum + other.ratingSum(), count + other.count());
    }
}
