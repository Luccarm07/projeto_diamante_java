package br.com.fiap.delivery.order.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketTests {

    private static final long ONE_SECOND = 1_000_000_000L;

    @Test
    void allowsTwentyRequestsThenBlocksWithinTheSameSecond() {
        AtomicLong clock = new AtomicLong(0);
        TokenBucket bucket = new TokenBucket(20, 20, clock::get);

        int allowed = 0;
        for (int i = 0; i < 50; i++) {
            if (bucket.tryConsume()) {
                allowed++;
            }
        }

        assertThat(allowed).isEqualTo(20);
    }

    @Test
    void refillsOverTime() {
        AtomicLong clock = new AtomicLong(0);
        TokenBucket bucket = new TokenBucket(20, 20, clock::get);
        for (int i = 0; i < 20; i++) {
            bucket.tryConsume();
        }
        assertThat(bucket.tryConsume()).isFalse();

        clock.addAndGet(ONE_SECOND / 2); // half a second -> 10 new tokens

        int allowed = 0;
        for (int i = 0; i < 20; i++) {
            if (bucket.tryConsume()) {
                allowed++;
            }
        }
        assertThat(allowed).isEqualTo(10);
    }

    @Test
    void neverStoresMoreThanTheCapacity() {
        AtomicLong clock = new AtomicLong(0);
        TokenBucket bucket = new TokenBucket(20, 20, clock::get);

        clock.addAndGet(10 * ONE_SECOND);

        int allowed = 0;
        for (int i = 0; i < 100; i++) {
            if (bucket.tryConsume()) {
                allowed++;
            }
        }
        assertThat(allowed).isEqualTo(20);
    }
}
