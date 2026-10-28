package com.emilhuseynov.accesspass.holder;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class HolderDtos {

    private HolderDtos() {
    }

    public record CreateHolderRequest(
            @NotBlank @Size(max = 160) String fullName,
            @Email @Size(max = 254) String email,
            @Size(max = 64) String externalRef) {
    }

    public record HolderResponse(
            Long id, String fullName, String email, String externalRef, boolean active, Instant createdAt) {

        static HolderResponse from(Holder holder) {
            return new HolderResponse(holder.getId(), holder.getFullName(), holder.getEmail(),
                    holder.getExternalRef(), holder.isActive(), holder.getCreatedAt());
        }
    }
}
