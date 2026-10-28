package com.emilhuseynov.accesspass.scan;

import java.time.Instant;
import java.util.UUID;

public record ScanAuditFilter(
        Long gateId,
        Long deviceId,
        UUID passId,
        Long holderId,
        ScanOutcome outcome,
        DenyReason reason,
        Instant from,
        Instant to) {
}
