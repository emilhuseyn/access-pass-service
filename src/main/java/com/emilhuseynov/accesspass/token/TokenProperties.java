package com.emilhuseynov.accesspass.token;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("access-pass.tokens")
public record TokenProperties(@NotBlank String activeKeyId, @NotEmpty Map<String, String> keys) {
}
