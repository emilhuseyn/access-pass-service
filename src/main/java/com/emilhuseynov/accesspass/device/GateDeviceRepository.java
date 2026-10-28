package com.emilhuseynov.accesspass.device;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GateDeviceRepository extends JpaRepository<GateDevice, Long> {

    @Query("""
            select new com.emilhuseynov.accesspass.device.DevicePrincipal(d.id, d.gate.id, d.name)
              from GateDevice d
             where d.apiKeyHash = :apiKeyHash
               and d.active = true
            """)
    Optional<DevicePrincipal> findActivePrincipalByApiKeyHash(@Param("apiKeyHash") String apiKeyHash);

    List<GateDevice> findByGateIdOrderByName(Long gateId);
}
