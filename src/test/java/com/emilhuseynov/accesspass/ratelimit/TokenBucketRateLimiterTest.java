package com.emilhuseynov.accesspass.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class TokenBucketRateLimiterTest {

    private final AtomicLong nanos = new AtomicLong();
    private final TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(3, 2.0, nanos::get);

    @Test
    void allowsABurstUpToCapacityThenRejects() {
        assertThat(IntStream.range(0, 3).mapToObj(i -> limiter.tryAcquire("device:1"))).containsOnly(true);
        assertThat(limiter.tryAcquire("device:1")).isFalse();
    }

    @Test
    void refillsAtTheConfiguredRate() {
        drain("device:1");

        advance(Duration.ofMillis(499));
        assertThat(limiter.tryAcquire("device:1")).isFalse();

        advance(Duration.ofMillis(1));
        assertThat(limiter.tryAcquire("device:1")).isTrue();
        assertThat(limiter.tryAcquire("device:1")).isFalse();
    }

    @Test
    void neverRefillsBeyondCapacity() {
        drain("device:1");

        advance(Duration.ofMinutes(10));

        assertThat(IntStream.range(0, 3).mapToObj(i -> limiter.tryAcquire("device:1"))).containsOnly(true);
        assertThat(limiter.tryAcquire("device:1")).isFalse();
    }

    @Test
    void keepsSeparateBucketsPerKey() {
        drain("device:1");

        assertThat(limiter.tryAcquire("device:2")).isTrue();
    }

    private void drain(String key) {
        IntStream.range(0, 3).forEach(i -> limiter.tryAcquire(key));
    }

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }
}
