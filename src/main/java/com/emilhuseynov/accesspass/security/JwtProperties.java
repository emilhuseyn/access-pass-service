package com.emilhuseynov.accesspass.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("access-pass.jwt")
public record JwtProperties(@NotBlank String issuer, @NotBlank String secret, @NotNull Duration ttl) {
}
