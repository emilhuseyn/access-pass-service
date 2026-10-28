package com.emilhuseynov.accesspass.scan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emilhuseynov.accesspass.device.DevicePrincipal;
import com.emilhuseynov.accesspass.holder.Holder;
import com.emilhuseynov.accesspass.pass.AccessPass;
import com.emilhuseynov.accesspass.pass.AccessPassRepository;
import com.emilhuseynov.accesspass.pass.PassStatus;
import com.emilhuseynov.accesspass.policy.DailyWindow;
import com.emilhuseynov.accesspass.policy.PassPolicy;
import com.emilhuseynov.accesspass.site.Gate;
import com.emilhuseynov.accesspass.site.Site;
import com.emilhuseynov.accesspass.token.PassClaims;
import com.emilhuseynov.accesspass.token.PassTokenCodec;
import com.emilhuseynov.accesspass.token.SigningKeyRing;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
class ScanServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-28T10:00:00Z");
    private static final ZoneId BAKU = ZoneId.of("Asia/Baku");
    private static final Long GATE_ID = 10L;
    private static final Long OTHER_GATE_ID = 11L;
    private static final PassTokenCodec CODEC = new PassTokenCodec(new SigningKeyRing("k1", Map.of("k1", key(1))));

    @Mock
    private AccessPassRepository passes;

    @Mock
    private ScanAuditRepository auditLog;

    private ScanService scanService;
    private Site site;
    private Gate gate;
    private Holder holder;
    private final DevicePrincipal device = new DevicePrincipal(1L, GATE_ID, "Turnstile A");

    @BeforeEach
    void setUp() {
        scanService = new ScanService(CODEC, passes, auditLog, TransactionOperations.withoutTransaction(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        site = new Site("Head office", BAKU, NOW);
        gate = withId(new Gate(site, "MAIN", "Main entrance", NOW), GATE_ID);
        holder = withId(new Holder("Aysel Mammadova", null, null, NOW), 5L);
    }

    @Test
    void grantsValidPassAndConsumesOneUse() {
        AccessPass pass = pass(policy(null, null));
        when(passes.consumeUse(pass.getId(), PassStatus.ACTIVE)).thenReturn(1);

        ScanDecision decision = scan(pass);

        assertThat(decision.outcome()).isEqualTo(ScanOutcome.GRANTED);
        assertThat(decision.reason()).isNull();
        assertThat(decision.holderName()).isEqualTo("Aysel Mammadova");
        ScanAuditEntry entry = savedAuditEntry();
        assertThat(entry.getOutcome()).isEqualTo(ScanOutcome.GRANTED);
        assertThat(entry.getPassId()).isEqualTo(pass.getId());
        assertThat(entry.getHolderId()).isEqualTo(5L);
        assertThat(entry.getDeviceId()).isEqualTo(1L);
        assertThat(entry.getGateId()).isEqualTo(GATE_ID);
        assertThat(entry.getOccurredAt()).isEqualTo(NOW);
        assertThat(entry.getTokenFingerprint()).matches("[0-9a-f]{16}");
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidTokens")
    void deniesInvalidTokensWithoutTouchingThePassStore(String token, DenyReason expected) {
        ScanDecision decision = scanService.scan(device, token);

        assertThat(decision.outcome()).isEqualTo(ScanOutcome.DENIED);
        assertThat(decision.reason()).isEqualTo(expected);
        assertThat(decision.passId()).isNull();
        verifyNoInteractions(passes);
        assertThat(savedAuditEntry().getReason()).isEqualTo(expected);
    }

    static Stream<Arguments> invalidTokens() {
        PassClaims claims = new PassClaims(UUID.randomUUID(), 5L, NOW.plus(Duration.ofHours(1)), "nonce-1");
        PassTokenCodec otherIssuer = new PassTokenCodec(new SigningKeyRing("k9", Map.of("k9", key(9))));
        String valid = CODEC.sign(claims, "k1");
        String[] parts = valid.split("\\.");
        parts[3] = (parts[3].charAt(0) == 'A' ? "B" : "A") + parts[3].substring(1);
        PassClaims expired = new PassClaims(claims.passId(), 5L, NOW.minusSeconds(1), "nonce-1");

        return Stream.of(
                Arguments.of("not-a-pass-token", DenyReason.MALFORMED_TOKEN),
                Arguments.of(otherIssuer.sign(claims, "k9"), DenyReason.UNKNOWN_SIGNING_KEY),
                Arguments.of(String.join(".", parts), DenyReason.INVALID_SIGNATURE),
                Arguments.of(CODEC.sign(expired, "k1"), DenyReason.EXPIRED));
    }

    @Test
    void deniesValidlySignedTokenForUnknownPassAndKeepsItsIdForInvestigation() {
        PassClaims claims = new PassClaims(UUID.randomUUID(), 5L, NOW.plus(Duration.ofHours(1)), "nonce-1");
        when(passes.findDetailedById(claims.passId())).thenReturn(Optional.empty());

        ScanDecision decision = scanService.scan(device, CODEC.sign(claims, "k1"));

        assertThat(decision.reason()).isEqualTo(DenyReason.UNKNOWN_PASS);
        assertThat(savedAuditEntry().getPassId()).isEqualTo(claims.passId());
    }

    @Test
    void deniesTokenWhoseClaimsDoNotMatchTheStoredPass() {
        AccessPass pass = pass(policy(null, null));
        PassClaims stale = new PassClaims(pass.getId(), 5L, pass.getValidUntil(), "superseded-nonce");
        when(passes.findDetailedById(pass.getId())).thenReturn(Optional.of(pass));

        assertDenied(scanService.scan(device, CODEC.sign(stale, "k1")), DenyReason.TOKEN_MISMATCH);
    }

    @Test
    void deniesRevokedPass() {
        AccessPass pass = pass(policy(null, null));
        ReflectionTestUtils.setField(pass, "status", PassStatus.REVOKED);

        assertDenied(scan(pass), DenyReason.REVOKED);
    }

    @Test
    void deniesPassOfDeactivatedHolder() {
        AccessPass pass = pass(policy(null, null));
        holder.deactivate();

        assertDenied(scan(pass), DenyReason.HOLDER_INACTIVE);
    }

    @Test
    void deniesPassBeforeItsValidityStarts() {
        AccessPass pass = pass(policy(null, null), NOW.plus(Duration.ofHours(2)), NOW.plus(Duration.ofDays(1)));

        assertDenied(scan(pass), DenyReason.NOT_YET_VALID);
    }

    @Test
    void deniesGateOutsideThePolicy() {
        AccessPass pass = pass(policy(null, null));
        DevicePrincipal otherGateDevice = new DevicePrincipal(2L, OTHER_GATE_ID, "Turnstile B");
        when(passes.findDetailedById(pass.getId())).thenReturn(Optional.of(pass));

        assertDenied(scanService.scan(otherGateDevice, CODEC.sign(pass.claims(), "k1")), DenyReason.GATE_NOT_ALLOWED);
    }

    @Test
    void deniesScanOutsideTheDailyWindowInTheSiteTimeZone() {
        AccessPass pass = pass(policy(null, new DailyWindow(LocalTime.of(8, 0), LocalTime.of(12, 0))));

        assertDenied(scan(pass), DenyReason.OUTSIDE_TIME_WINDOW);
    }

    @Test
    void evaluatesTheDailyWindowInSiteLocalTimeNotUtc() {
        AccessPass pass = pass(policy(null, new DailyWindow(LocalTime.of(13, 0), LocalTime.of(15, 0))));
        when(passes.consumeUse(pass.getId(), PassStatus.ACTIVE)).thenReturn(1);

        assertThat(scan(pass).outcome()).isEqualTo(ScanOutcome.GRANTED);
    }

    @Test
    void deniesExhaustedPassWithoutAttemptingToConsume() {
        AccessPass pass = pass(policy(1, null));
        ReflectionTestUtils.setField(pass, "useCount", 1);

        assertDenied(scan(pass), DenyReason.USES_EXHAUSTED);
        verify(passes, never()).consumeUse(any(), any());
    }

    @Test
    void deniesWhenAnotherScanConsumedTheLastUseFirst() {
        AccessPass pass = pass(policy(1, null));
        when(passes.consumeUse(pass.getId(), PassStatus.ACTIVE)).thenReturn(0);

        assertDenied(scan(pass), DenyReason.USES_EXHAUSTED);
    }

    @Test
    void deniesAndStillAuditsWhenTheDatabaseReportsALockConflict() {
        AccessPass pass = pass(policy(1, null));
        when(passes.consumeUse(pass.getId(), PassStatus.ACTIVE))
                .thenThrow(new CannotAcquireLockException("lock timeout"));

        ScanDecision decision = scan(pass);

        assertDenied(decision, DenyReason.CONCURRENT_SCAN);
        assertThat(decision.passId()).isEqualTo(pass.getId());
    }

    @Test
    void everyDenyReasonIsExercisedByThisSuite() {
        assertThat(Arrays.stream(DenyReason.values()).map(Enum::name)).containsExactlyInAnyOrder(
                "MALFORMED_TOKEN", "UNKNOWN_SIGNING_KEY", "INVALID_SIGNATURE", "EXPIRED", "UNKNOWN_PASS",
                "TOKEN_MISMATCH", "REVOKED", "HOLDER_INACTIVE", "NOT_YET_VALID", "GATE_NOT_ALLOWED",
                "OUTSIDE_TIME_WINDOW", "USES_EXHAUSTED", "CONCURRENT_SCAN");
    }

    private ScanDecision scan(AccessPass pass) {
        when(passes.findDetailedById(pass.getId())).thenReturn(Optional.of(pass));
        return scanService.scan(device, CODEC.sign(pass.claims(), "k1"));
    }

    private void assertDenied(ScanDecision decision, DenyReason reason) {
        assertThat(decision.outcome()).isEqualTo(ScanOutcome.DENIED);
        assertThat(decision.reason()).isEqualTo(reason);
        assertThat(decision.holderName()).isNull();
        ScanAuditEntry entry = savedAuditEntry();
        assertThat(entry.getOutcome()).isEqualTo(ScanOutcome.DENIED);
        assertThat(entry.getReason()).isEqualTo(reason);
    }

    private ScanAuditEntry savedAuditEntry() {
        ArgumentCaptor<ScanAuditEntry> captor = ArgumentCaptor.forClass(ScanAuditEntry.class);
        verify(auditLog).save(captor.capture());
        return captor.getValue();
    }

    private PassPolicy policy(Integer maxUses, DailyWindow window) {
        return new PassPolicy(site, "Staff", List.of(gate), window, maxUses, NOW);
    }

    private AccessPass pass(PassPolicy policy) {
        return pass(policy, NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(1)));
    }

    private AccessPass pass(PassPolicy policy, Instant validFrom, Instant validUntil) {
        return new AccessPass(UUID.randomUUID(), holder, policy, validFrom, validUntil, "k1", "nonce-1", NOW,
                "tester");
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static SecretKey key(int fill) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) fill);
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
