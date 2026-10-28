package com.emilhuseynov.accesspass.support;

import com.emilhuseynov.accesspass.device.DeviceService;
import com.emilhuseynov.accesspass.device.DeviceService.RegisteredDevice;
import com.emilhuseynov.accesspass.holder.Holder;
import com.emilhuseynov.accesspass.holder.HolderService;
import com.emilhuseynov.accesspass.pass.IssuedPass;
import com.emilhuseynov.accesspass.pass.PassService;
import com.emilhuseynov.accesspass.policy.PassPolicy;
import com.emilhuseynov.accesspass.policy.PolicyDtos.CreatePolicyRequest;
import com.emilhuseynov.accesspass.policy.PolicyService;
import com.emilhuseynov.accesspass.security.JwtTokenIssuer;
import com.emilhuseynov.accesspass.site.Gate;
import com.emilhuseynov.accesspass.site.Site;
import com.emilhuseynov.accesspass.site.SiteService;
import com.emilhuseynov.accesspass.staff.StaffRole;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class TestFixtures {

    private final SiteService siteService;
    private final PolicyService policyService;
    private final HolderService holderService;
    private final PassService passService;
    private final DeviceService deviceService;
    private final JwtTokenIssuer jwtTokenIssuer;

    TestFixtures(SiteService siteService, PolicyService policyService, HolderService holderService,
                 PassService passService, DeviceService deviceService, JwtTokenIssuer jwtTokenIssuer) {
        this.siteService = siteService;
        this.policyService = policyService;
        this.holderService = holderService;
        this.passService = passService;
        this.deviceService = deviceService;
        this.jwtTokenIssuer = jwtTokenIssuer;
    }

    public Site site() {
        return siteService.createSite("Campus " + suffix(), "Asia/Baku");
    }

    public Gate gate(Site site) {
        return siteService.createGate(site.getId(), "G-" + suffix(), "Main entrance");
    }

    public Holder holder() {
        String suffix = suffix();
        return holderService.create("Holder " + suffix, suffix + "@example.org", null);
    }

    public PassPolicy policy(Site site, List<Gate> gates, Integer maxUses) {
        Set<Long> gateIds = gates.stream().map(Gate::getId).collect(Collectors.toSet());
        return policyService.create(site.getId(),
                new CreatePolicyRequest("Policy " + suffix(), gateIds, null, null, maxUses));
    }

    public IssuedPass pass(Holder holder, PassPolicy policy) {
        return passService.issue(holder.getId(), policy.getId(), null, Instant.now().plus(Duration.ofDays(1)),
                "fixtures");
    }

    public RegisteredDevice device(Gate gate) {
        return deviceService.register(gate.getId(), "Turnstile " + suffix());
    }

    public String bearer(StaffRole role) {
        return "Bearer " + jwtTokenIssuer.issue(role.name().toLowerCase() + "-" + suffix(), List.of(role.name()))
                .value();
    }

    public Scenario scenario(Integer maxUses) {
        Site site = site();
        Gate gate = gate(site);
        PassPolicy policy = policy(site, List.of(gate), maxUses);
        Holder holder = holder();
        return new Scenario(site, gate, policy, holder, device(gate), pass(holder, policy));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public record Scenario(Site site, Gate gate, PassPolicy policy, Holder holder, RegisteredDevice device,
                           IssuedPass pass) {

        public String token() {
            return pass.token();
        }

        public String deviceKey() {
            return device.apiKey();
        }
    }
}
