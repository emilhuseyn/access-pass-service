package com.emilhuseynov.accesspass.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class StaffDtos {

    private StaffDtos() {
    }

    public record TokenRequest(@NotBlank @Size(max = 64) String username, @NotBlank @Size(max = 128) String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }

    public record CreateStaffRequest(
            @NotBlank @Size(min = 3, max = 64) @Pattern(regexp = "[a-z0-9._-]+") String username,
            @NotBlank @Size(min = 12, max = 128) String password,
            @NotNull StaffRole role) {
    }

    public record StaffResponse(Long id, String username, StaffRole role, boolean enabled, Instant createdAt) {

        static StaffResponse from(StaffAccount account) {
            return new StaffResponse(account.getId(), account.getUsername(), account.getRole(), account.isEnabled(),
                    account.getCreatedAt());
        }
    }
}
