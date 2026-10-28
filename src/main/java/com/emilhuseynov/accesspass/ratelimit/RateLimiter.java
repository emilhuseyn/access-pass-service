package com.emilhuseynov.accesspass.ratelimit;

public interface RateLimiter {

    boolean tryAcquire(String key);
}
