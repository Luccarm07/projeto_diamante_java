package br.com.fiap.delivery.review.buffer;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewBufferTests {

    @Test
    void accumulatesSumAndCountPerDish() {
        ReviewBuffer buffer = new ReviewBuffer();
        buffer.add(1L, "House Burger", 5);
        buffer.add(1L, "House Burger", 3);
        buffer.add(2L, "Garden Bowl", 4);

        Map<Long, PendingReviews> drained = buffer.drain();

        assertThat(drained.get(1L)).isEqualTo(new PendingReviews("House Burger", 8, 2));
        assertThat(drained.get(2L)).isEqualTo(new PendingReviews("Garden Bowl", 4, 1));
        assertThat(buffer.isEmpty()).isTrue();
    }

    @Test
    void noReviewIsLostWhenDrainingWhileWriting() throws Exception {
        ReviewBuffer buffer = new ReviewBuffer();
        int writers = 8;
        int reviewsPerWriter = 5_000;
        ExecutorService pool = Executors.newFixedThreadPool(writers);
        CountDownLatch done = new CountDownLatch(writers);
        AtomicBoolean writing = new AtomicBoolean(true);

        for (int w = 0; w < writers; w++) {
            pool.submit(() -> {
                for (int i = 0; i < reviewsPerWriter; i++) {
                    buffer.add((long) (i % 3) + 1, "Dish", 4);
                }
                done.countDown();
            });
        }

        // Simulates the scheduler flushing while the listener keeps writing
        Map<Long, long[]> totals = new HashMap<>();
        Thread flusher = new Thread(() -> {
            while (writing.get() || !buffer.isEmpty()) {
                buffer.drain().forEach((dishId, p) -> {
                    long[] t = totals.computeIfAbsent(dishId, k -> new long[2]);
                    t[0] += p.ratingSum();
                    t[1] += p.count();
                });
            }
        });
        flusher.start();

        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        writing.set(false);
        flusher.join(10_000);
        pool.shutdown();

        long totalCount = totals.values().stream().mapToLong(t -> t[1]).sum();
        long totalSum = totals.values().stream().mapToLong(t -> t[0]).sum();
        assertThat(totalCount).isEqualTo((long) writers * reviewsPerWriter);
        assertThat(totalSum).isEqualTo(4L * writers * reviewsPerWriter);
    }

    @Test
    void restorePutsReviewsBack() {
        ReviewBuffer buffer = new ReviewBuffer();
        buffer.add(1L, "House Burger", 5);
        Map<Long, PendingReviews> drained = buffer.drain();
        buffer.add(1L, "House Burger", 1);

        buffer.restore(drained);

        assertThat(buffer.drain().get(1L)).isEqualTo(new PendingReviews("House Burger", 6, 2));
    }
}
