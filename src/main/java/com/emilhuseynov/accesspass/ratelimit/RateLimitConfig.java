package com.emilhuseynov.accesspass.ratelimit;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration(proxyBeanMethods = false)
class RateLimitConfig {

    @Bean
    RateLimiter scanRateLimiter(ScanRateLimitProperties properties) {
        return new TokenBucketRateLimiter(properties.capacity(), properties.refillPerSecond(), System::nanoTime);
    }

    @Validated
    @ConfigurationProperties("access-pass.scan.rate-limit")
    record ScanRateLimitProperties(
            @DefaultValue("10") @Min(1) int capacity,
            @DefaultValue("5") @Positive double refillPerSecond) {
    }
}
