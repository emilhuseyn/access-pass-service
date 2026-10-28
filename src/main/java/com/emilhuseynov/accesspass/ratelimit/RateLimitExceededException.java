package com.emilhuseynov.accesspass.ratelimit;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Too many scans from this device, slow down");
    }
}
