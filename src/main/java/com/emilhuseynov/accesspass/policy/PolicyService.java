package com.emilhuseynov.accesspass.policy;

import com.emilhuseynov.accesspass.common.ConflictException;
import com.emilhuseynov.accesspass.common.InvalidRequestException;
import com.emilhuseynov.accesspass.common.NotFoundException;
import com.emilhuseynov.accesspass.policy.PolicyDtos.CreatePolicyRequest;
import com.emilhuseynov.accesspass.site.Gate;
import com.emilhuseynov.accesspass.site.GateRepository;
import com.emilhuseynov.accesspass.site.Site;
import com.emilhuseynov.accesspass.site.SiteService;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyService {

    private final PassPolicyRepository policies;
    private final GateRepository gates;
    private final SiteService siteService;
    private final Clock clock;

    public PolicyService(PassPolicyRepository policies, GateRepository gates, SiteService siteService, Clock clock) {
        this.policies = policies;
        this.gates = gates;
        this.siteService = siteService;
        this.clock = clock;
    }

    @Transactional
    public PassPolicy create(Long siteId, CreatePolicyRequest request) {
        Site site = siteService.getSite(siteId);
        if (policies.existsBySiteIdAndNameIgnoreCase(siteId, request.name())) {
            throw new ConflictException("A policy named '" + request.name() + "' already exists at site " + siteId);
        }
        List<Gate> allowedGates = gates.findBySiteIdAndIdIn(siteId, request.gateIds());
        if (allowedGates.size() != request.gateIds().size()) {
            throw new InvalidRequestException("Every gate in a policy must exist and belong to site " + siteId);
        }
        DailyWindow window = toWindow(request);
        return policies.save(new PassPolicy(site, request.name(), allowedGates, window, request.maxUses(),
                clock.instant()));
    }

    @Transactional(readOnly = true)
    public PassPolicy get(Long id) {
        return policies.findWithGatesById(id).orElseThrow(() -> new NotFoundException("Policy", id));
    }

    @Transactional(readOnly = true)
    public List<PassPolicy> listForSite(Long siteId) {
        siteService.getSite(siteId);
        return policies.findBySiteIdOrderByName(siteId);
    }

    private static DailyWindow toWindow(CreatePolicyRequest request) {
        if (request.windowStart() == null && request.windowEnd() == null) {
            return null;
        }
        if (request.windowStart() == null || request.windowEnd() == null) {
            throw new InvalidRequestException("windowStart and windowEnd must be given together");
        }
        if (request.windowStart().equals(request.windowEnd())) {
            throw new InvalidRequestException("windowStart and windowEnd must differ");
        }
        return new DailyWindow(request.windowStart(), request.windowEnd());
    }
}
