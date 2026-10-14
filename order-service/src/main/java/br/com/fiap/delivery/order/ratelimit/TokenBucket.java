package br.com.fiap.delivery.order.ratelimit;

import java.util.function.LongSupplier;

/**
 * Minimal thread-safe token bucket. It holds up to {@code capacity} tokens and refills
 * {@code tokensPerSecond} continuously; each request consumes one token.
 */
public class TokenBucket {

    private static final double NANOS_PER_SECOND = 1_000_000_000d;

    private final long capacity;
    private final double tokensPerNano;
    private final LongSupplier nanoClock;

    private double availableTokens;
    private long lastRefillNanos;

    public TokenBucket(long capacity, long tokensPerSecond) {
        this(capacity, tokensPerSecond, System::nanoTime);
    }

    TokenBucket(long capacity, long tokensPerSecond, LongSupplier nanoClock) {
        if (capacity <= 0 || tokensPerSecond <= 0) {
            throw new IllegalArgumentException("capacity and tokensPerSecond must be positive");
        }
        this.capacity = capacity;
        this.tokensPerNano = tokensPerSecond / NANOS_PER_SECOND;
        this.nanoClock = nanoClock;
        this.availableTokens = capacity;
        this.lastRefillNanos = nanoClock.getAsLong();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (availableTokens >= 1) {
            availableTokens -= 1;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = nanoClock.getAsLong();
        long elapsed = now - lastRefillNanos;
        if (elapsed > 0) {
            availableTokens = Math.min(capacity, availableTokens + elapsed * tokensPerNano);
            lastRefillNanos = now;
        }
    }
}
