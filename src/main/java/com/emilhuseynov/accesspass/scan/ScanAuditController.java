package com.emilhuseynov.accesspass.scan;

import com.emilhuseynov.accesspass.common.InvalidRequestException;
import com.emilhuseynov.accesspass.common.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit/scans")
class ScanAuditController {

    private final ScanAuditRepository auditLog;

    ScanAuditController(ScanAuditRepository auditLog) {
        this.auditLog = auditLog;
    }

    @GetMapping
    PageResponse<ScanAuditResponse> search(
            @ModelAttribute ScanAuditFilter filter,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        if (filter.from() != null && filter.to() != null && !filter.from().isBefore(filter.to())) {
            throw new InvalidRequestException("'from' must be before 'to'");
        }
        return PageResponse.of(auditLog.search(filter, PageRequest.of(page, size)), ScanAuditResponse::from);
    }

    record ScanAuditResponse(
            Long id,
            Instant occurredAt,
            Long deviceId,
            Long gateId,
            UUID passId,
            Long holderId,
            ScanOutcome outcome,
            DenyReason reason,
            String tokenFingerprint) {

        static ScanAuditResponse from(ScanAuditEntry entry) {
            return new ScanAuditResponse(entry.getId(), entry.getOccurredAt(), entry.getDeviceId(),
                    entry.getGateId(), entry.getPassId(), entry.getHolderId(), entry.getOutcome(), entry.getReason(),
                    entry.getTokenFingerprint());
        }
    }
}
