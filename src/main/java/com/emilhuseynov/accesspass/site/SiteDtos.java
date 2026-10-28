package com.emilhuseynov.accesspass.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class SiteDtos {

    private SiteDtos() {
    }

    public record CreateSiteRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 64) String timeZone) {
    }

    public record SiteResponse(Long id, String name, String timeZone, Instant createdAt) {

        static SiteResponse from(Site site) {
            return new SiteResponse(site.getId(), site.getName(), site.getZone().getId(), site.getCreatedAt());
        }
    }

    public record CreateGateRequest(
            @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
            @NotBlank @Size(max = 120) String name) {
    }

    public record GateResponse(Long id, Long siteId, String code, String name, Instant createdAt) {

        static GateResponse from(Gate gate) {
            return new GateResponse(gate.getId(), gate.getSite().getId(), gate.getCode(), gate.getName(),
                    gate.getCreatedAt());
        }
    }
}
