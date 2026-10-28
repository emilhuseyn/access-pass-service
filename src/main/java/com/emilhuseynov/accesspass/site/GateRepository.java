package com.emilhuseynov.accesspass.site;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GateRepository extends JpaRepository<Gate, Long> {

    List<Gate> findBySiteIdOrderByCode(Long siteId);

    List<Gate> findBySiteIdAndIdIn(Long siteId, Collection<Long> ids);

    boolean existsBySiteIdAndCodeIgnoreCase(Long siteId, String code);
}
