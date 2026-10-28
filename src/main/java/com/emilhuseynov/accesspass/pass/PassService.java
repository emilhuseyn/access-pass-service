package com.emilhuseynov.accesspass.pass;

import com.emilhuseynov.accesspass.common.ConflictException;
import com.emilhuseynov.accesspass.common.InvalidRequestException;
import com.emilhuseynov.accesspass.common.NotFoundException;
import com.emilhuseynov.accesspass.holder.Holder;
import com.emilhuseynov.accesspass.holder.HolderService;
import com.emilhuseynov.accesspass.policy.PassPolicy;
import com.emilhuseynov.accesspass.policy.PolicyService;
import com.emilhuseynov.accesspass.token.PassTokenCodec;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassService {

    private static final int NONCE_BYTES = 16;

    private final AccessPassRepository passes;
    private final HolderService holderService;
    private final PolicyService policyService;
    private final PassTokenCodec tokenCodec;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public PassService(AccessPassRepository passes, HolderService holderService, PolicyService policyService,
                       PassTokenCodec tokenCodec, Clock clock) {
        this.passes = passes;
        this.holderService = holderService;
        this.policyService = policyService;
        this.tokenCodec = tokenCodec;
        this.clock = clock;
    }

    @Transactional
    public IssuedPass issue(Long holderId, Long policyId, Instant requestedValidFrom, Instant requestedValidUntil,
                            String issuedBy) {
        Holder holder = holderService.get(holderId);
        if (!holder.isActive()) {
            throw new ConflictException("Holder " + holderId + " is deactivated");
        }
        PassPolicy policy = policyService.get(policyId);
        Instant now = clock.instant();
        Instant validFrom = requestedValidFrom == null ? now : requestedValidFrom;
        Instant validUntil = requestedValidUntil.truncatedTo(ChronoUnit.SECONDS);
        if (!validUntil.isAfter(validFrom)) {
            throw new InvalidRequestException("validUntil must be after validFrom");
        }
        if (!validUntil.isAfter(now)) {
            throw new InvalidRequestException("validUntil must be in the future");
        }
        AccessPass pass = passes.save(new AccessPass(UUID.randomUUID(), holder, policy, validFrom, validUntil,
                tokenCodec.activeKeyId(), newNonce(), now, issuedBy));
        return new IssuedPass(pass, tokenCodec.sign(pass.claims(), pass.getKeyId()));
    }

    @Transactional(readOnly = true)
    public AccessPass get(UUID id) {
        return passes.findDetailedById(id).orElseThrow(() -> new NotFoundException("Pass", id));
    }

    @Transactional(readOnly = true)
    public Page<AccessPass> listForHolder(Long holderId, Pageable pageable) {
        return passes.findByHolderId(holderId, pageable);
    }

    @Transactional
    public AccessPass revoke(UUID id, String reason, String revokedBy) {
        AccessPass pass = get(id);
        if (pass.isRevoked()) {
            throw new ConflictException("Pass " + id + " is already revoked");
        }
        pass.revoke(clock.instant(), revokedBy, reason);
        return pass;
    }

    @Transactional(readOnly = true)
    public String currentToken(UUID id) {
        AccessPass pass = get(id);
        if (pass.isRevoked()) {
            throw new ConflictException("Pass " + id + " is revoked");
        }
        if (!clock.instant().isBefore(pass.getValidUntil())) {
            throw new ConflictException("Pass " + id + " has expired");
        }
        if (!tokenCodec.hasKey(pass.getKeyId())) {
            throw new ConflictException("The signing key of pass " + id + " has been retired, issue a new pass");
        }
        return tokenCodec.sign(pass.claims(), pass.getKeyId());
    }

    private String newNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
