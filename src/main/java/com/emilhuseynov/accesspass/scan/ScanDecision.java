package com.emilhuseynov.accesspass.scan;

import com.emilhuseynov.accesspass.pass.AccessPass;
import java.util.UUID;

public record ScanDecision(ScanOutcome outcome, DenyReason reason, UUID passId, Long holderId, String holderName) {

    static ScanDecision granted(AccessPass pass) {
        return new ScanDecision(ScanOutcome.GRANTED, null, pass.getId(), pass.getHolder().getId(),
                pass.getHolder().getFullName());
    }

    static ScanDecision denied(DenyReason reason, AccessPass pass) {
        return new ScanDecision(ScanOutcome.DENIED, reason, pass.getId(), pass.getHolder().getId(), null);
    }

    static ScanDecision denied(DenyReason reason, UUID passId, Long holderId) {
        return new ScanDecision(ScanOutcome.DENIED, reason, passId, holderId, null);
    }

    public boolean isGranted() {
        return outcome == ScanOutcome.GRANTED;
    }
}
