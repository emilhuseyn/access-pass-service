package com.emilhuseynov.accesspass.scan;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.emilhuseynov.accesspass.device.DevicePrincipal;
import com.emilhuseynov.accesspass.pass.AccessPass;
import com.emilhuseynov.accesspass.pass.AccessPassRepository;
import com.emilhuseynov.accesspass.pass.PassStatus;
import com.emilhuseynov.accesspass.token.PassClaims;
import com.emilhuseynov.accesspass.token.PassTokenCodec;
import com.emilhuseynov.accesspass.token.TokenVerification;
import com.emilhuseynov.accesspass.token.TokenVerification.Invalid;
import com.emilhuseynov.accesspass.token.TokenVerification.Valid;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private final PassTokenCodec tokenCodec;
    private final AccessPassRepository passes;
    private final ScanAuditRepository auditLog;
    private final TransactionOperations transactions;
    private final Clock clock;

    public ScanService(PassTokenCodec tokenCodec, AccessPassRepository passes, ScanAuditRepository auditLog,
                       TransactionOperations transactions, Clock clock) {
        this.tokenCodec = tokenCodec;
        this.passes = passes;
        this.auditLog = auditLog;
        this.transactions = transactions;
        this.clock = clock;
    }

    public ScanDecision scan(DevicePrincipal device, String token) {
        Instant now = clock.instant();
        String fingerprint = fingerprint(token);
        TokenVerification verification = tokenCodec.verify(token, now);
        ScanDecision decision;
        try {
            decision = transactions.execute(status ->
                    audit(device, now, fingerprint, decide(verification, device.gateId(), now)));
        } catch (ConcurrencyFailureException ex) {
            PassClaims claims = ((Valid) verification).claims();
            ScanDecision lostRace = ScanDecision.denied(DenyReason.CONCURRENT_SCAN, claims.passId(), claims.holderId());
            decision = transactions.execute(status -> audit(device, now, fingerprint, lostRace));
        }
        log.info("Scan at gate {} by device {}: outcome={} reason={} pass={} tokenFingerprint={}", device.gateId(),
                device.deviceId(), decision.outcome(), decision.reason(), decision.passId(), fingerprint);
        return decision;
    }

    private ScanDecision decide(TokenVerification verification, Long gateId, Instant now) {
        return switch (verification) {
            case Invalid(TokenVerification.Failure failure) -> ScanDecision.denied(DenyReason.of(failure), null, null);
            case Valid valid -> passes.findDetailedById(valid.claims().passId())
                    .map(pass -> evaluateAndConsume(pass, valid.claims(), gateId, now))
                    .orElseGet(() -> ScanDecision.denied(DenyReason.UNKNOWN_PASS, valid.claims().passId(),
                            valid.claims().holderId()));
        };
    }

    private ScanDecision evaluateAndConsume(AccessPass pass, PassClaims claims, Long gateId, Instant now) {
        return PassRules.firstViolation(pass, claims, gateId, now)
                .map(reason -> ScanDecision.denied(reason, pass))
                .orElseGet(() -> passes.consumeUse(pass.getId(), PassStatus.ACTIVE) == 1
                        ? ScanDecision.granted(pass)
                        : ScanDecision.denied(DenyReason.USES_EXHAUSTED, pass));
    }

    private ScanDecision audit(DevicePrincipal device, Instant now, String fingerprint, ScanDecision decision) {
        auditLog.save(new ScanAuditEntry(now, device.deviceId(), device.gateId(), decision, fingerprint));
        return decision;
    }

    private static String fingerprint(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
