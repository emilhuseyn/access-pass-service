package com.emilhuseynov.accesspass.scan;

import com.emilhuseynov.accesspass.device.DevicePrincipal;
import com.emilhuseynov.accesspass.ratelimit.RateLimitExceededException;
import com.emilhuseynov.accesspass.ratelimit.RateLimiter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scans")
@SecurityRequirement(name = "deviceKey")
class ScanController {

    private final ScanService scanService;
    private final RateLimiter rateLimiter;

    ScanController(ScanService scanService, RateLimiter rateLimiter) {
        this.scanService = scanService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    ScanResponse scan(@AuthenticationPrincipal DevicePrincipal device, @Valid @RequestBody ScanRequest request) {
        if (!rateLimiter.tryAcquire("device:" + device.deviceId())) {
            throw new RateLimitExceededException();
        }
        return ScanResponse.from(scanService.scan(device, request.token()));
    }

    record ScanRequest(@NotBlank @Size(max = 512) String token) {
    }

    record ScanResponse(ScanOutcome decision, DenyReason reason, UUID passId, String holderName) {

        static ScanResponse from(ScanDecision decision) {
            return new ScanResponse(decision.outcome(), decision.reason(), decision.passId(), decision.holderName());
        }
    }
}
