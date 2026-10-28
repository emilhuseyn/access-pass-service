package com.emilhuseynov.accesspass.site;

import com.emilhuseynov.accesspass.common.ConflictException;
import com.emilhuseynov.accesspass.common.InvalidRequestException;
import com.emilhuseynov.accesspass.common.NotFoundException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteService {

    private final SiteRepository sites;
    private final GateRepository gates;
    private final Clock clock;

    public SiteService(SiteRepository sites, GateRepository gates, Clock clock) {
        this.sites = sites;
        this.gates = gates;
        this.clock = clock;
    }

    @Transactional
    public Site createSite(String name, String timeZone) {
        if (sites.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A site named '" + name + "' already exists");
        }
        return sites.save(new Site(name, parseZone(timeZone), clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<Site> listSites() {
        return sites.findAll();
    }

    @Transactional(readOnly = true)
    public Site getSite(Long id) {
        return sites.findById(id).orElseThrow(() -> new NotFoundException("Site", id));
    }

    @Transactional
    public Gate createGate(Long siteId, String code, String name) {
        Site site = getSite(siteId);
        if (gates.existsBySiteIdAndCodeIgnoreCase(siteId, code)) {
            throw new ConflictException("Gate code '" + code + "' is already used at site " + siteId);
        }
        return gates.save(new Gate(site, code, name, clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<Gate> listGates(Long siteId) {
        getSite(siteId);
        return gates.findBySiteIdOrderByCode(siteId);
    }

    @Transactional(readOnly = true)
    public Gate getGate(Long id) {
        return gates.findById(id).orElseThrow(() -> new NotFoundException("Gate", id));
    }

    private static ZoneId parseZone(String timeZone) {
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException ex) {
            throw new InvalidRequestException("Unknown time zone '" + timeZone + "'");
        }
    }
}
