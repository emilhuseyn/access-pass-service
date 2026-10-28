package com.emilhuseynov.accesspass.policy;

import com.emilhuseynov.accesspass.site.Gate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public final class PolicyDtos {

    private PolicyDtos() {
    }

    public record CreatePolicyRequest(
            @NotBlank @Size(max = 120) String name,
            @NotEmpty Set<@NotNull Long> gateIds,
            LocalTime windowStart,
            LocalTime windowEnd,
            @Positive Integer maxUses) {
    }

    public record PolicyResponse(
            Long id,
            Long siteId,
            String name,
            List<Long> gateIds,
            LocalTime windowStart,
            LocalTime windowEnd,
            Integer maxUses,
            Instant createdAt) {

        static PolicyResponse from(PassPolicy policy) {
            DailyWindow window = policy.getWindow();
            return new PolicyResponse(
                    policy.getId(),
                    policy.getSite().getId(),
                    policy.getName(),
                    policy.getGates().stream().map(Gate::getId).sorted().toList(),
                    window == null ? null : window.start(),
                    window == null ? null : window.end(),
                    policy.getMaxUses(),
                    policy.getCreatedAt());
        }
    }
}
