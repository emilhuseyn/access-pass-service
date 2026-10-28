package com.emilhuseynov.accesspass.policy;

import com.emilhuseynov.accesspass.policy.PolicyDtos.CreatePolicyRequest;
import com.emilhuseynov.accesspass.policy.PolicyDtos.PolicyResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PolicyController {

    private final PolicyService policyService;

    PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping("/api/v1/sites/{siteId}/policies")
    @ResponseStatus(HttpStatus.CREATED)
    PolicyResponse create(@PathVariable Long siteId, @Valid @RequestBody CreatePolicyRequest request) {
        return PolicyResponse.from(policyService.create(siteId, request));
    }

    @GetMapping("/api/v1/sites/{siteId}/policies")
    List<PolicyResponse> listForSite(@PathVariable Long siteId) {
        return policyService.listForSite(siteId).stream().map(PolicyResponse::from).toList();
    }

    @GetMapping("/api/v1/policies/{policyId}")
    PolicyResponse get(@PathVariable Long policyId) {
        return PolicyResponse.from(policyService.get(policyId));
    }
}
