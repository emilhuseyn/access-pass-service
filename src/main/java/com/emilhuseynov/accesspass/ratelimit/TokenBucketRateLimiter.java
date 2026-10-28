package com.emilhuseynov.accesspass.ratelimit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;

public final class TokenBucketRateLimiter implements RateLimiter {

    private static final double NANOS_PER_SECOND = 1_000_000_000d;

    private final double capacity;
    private final double tokensPerNano;
    private final LongSupplier nanoTime;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(int capacity, double refillPerSecond, LongSupplier nanoTime) {
        if (capacity < 1 || refillPerSecond <= 0) {
            throw new IllegalArgumentException("capacity must be >= 1 and refillPerSecond must be > 0");
        }
        this.capacity = capacity;
        this.tokensPerNano = refillPerSecond / NANOS_PER_SECOND;
        this.nanoTime = nanoTime;
    }

    @Override
    public boolean tryAcquire(String key) {
        return buckets.computeIfAbsent(key, k -> new Bucket(nanoTime.getAsLong())).tryConsume();
    }

    private final class Bucket {

        private double tokens = capacity;
        private long lastRefill;

        private Bucket(long createdAt) {
            this.lastRefill = createdAt;
        }

        synchronized boolean tryConsume() {
            long now = nanoTime.getAsLong();
            tokens = Math.min(capacity, tokens + (now - lastRefill) * tokensPerNano);
            lastRefill = now;
            if (tokens < 1) {
                return false;
            }
            tokens -= 1;
            return true;
        }
    }
}
