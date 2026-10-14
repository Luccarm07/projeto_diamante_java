package br.com.fiap.delivery.review.buffer;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, thread-safe accumulator of reviews per dish.
 *
 * add() uses ConcurrentHashMap.merge with immutable values, which is atomic per key.
 * drain() removes key by key (also atomic), so a review that arrives during a flush is
 * either in the drained snapshot or stays in the map for the next flush - never lost,
 * never counted twice.
 */
@Component
public class ReviewBuffer {

    private final ConcurrentHashMap<Long, PendingReviews> pending = new ConcurrentHashMap<>();

    public void add(Long dishId, String dishName, int rating) {
        pending.merge(dishId, PendingReviews.of(dishName, rating), PendingReviews::plus);
    }

    public Map<Long, PendingReviews> drain() {
        Map<Long, PendingReviews> snapshot = new HashMap<>();
        for (Long dishId : pending.keySet()) {
            PendingReviews removed = pending.remove(dishId);
            if (removed != null) {
                snapshot.put(dishId, removed);
            }
        }
        return snapshot;
    }

    /** Puts a drained snapshot back (used when the database write fails). */
    public void restore(Map<Long, PendingReviews> snapshot) {
        snapshot.forEach((dishId, reviews) -> pending.merge(dishId, reviews, PendingReviews::plus));
    }

    public boolean isEmpty() {
        return pending.isEmpty();
    }
}
