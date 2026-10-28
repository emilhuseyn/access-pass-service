package com.emilhuseynov.accesspass.site;

import com.emilhuseynov.accesspass.site.SiteDtos.CreateGateRequest;
import com.emilhuseynov.accesspass.site.SiteDtos.CreateSiteRequest;
import com.emilhuseynov.accesspass.site.SiteDtos.GateResponse;
import com.emilhuseynov.accesspass.site.SiteDtos.SiteResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sites")
class SiteController {

    private final SiteService siteService;

    SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SiteResponse createSite(@Valid @RequestBody CreateSiteRequest request) {
        return SiteResponse.from(siteService.createSite(request.name(), request.timeZone()));
    }

    @GetMapping
    List<SiteResponse> listSites() {
        return siteService.listSites().stream().map(SiteResponse::from).toList();
    }

    @GetMapping("/{siteId}")
    SiteResponse getSite(@PathVariable Long siteId) {
        return SiteResponse.from(siteService.getSite(siteId));
    }

    @PostMapping("/{siteId}/gates")
    @ResponseStatus(HttpStatus.CREATED)
    GateResponse createGate(@PathVariable Long siteId, @Valid @RequestBody CreateGateRequest request) {
        return GateResponse.from(siteService.createGate(siteId, request.code(), request.name()));
    }

    @GetMapping("/{siteId}/gates")
    List<GateResponse> listGates(@PathVariable Long siteId) {
        return siteService.listGates(siteId).stream().map(GateResponse::from).toList();
    }
}
