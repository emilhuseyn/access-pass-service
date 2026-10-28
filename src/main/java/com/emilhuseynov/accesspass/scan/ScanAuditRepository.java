package com.emilhuseynov.accesspass.scan;

import org.springframework.data.repository.Repository;

public interface ScanAuditRepository extends Repository<ScanAuditEntry, Long> {

    ScanAuditEntry save(ScanAuditEntry entry);
}
