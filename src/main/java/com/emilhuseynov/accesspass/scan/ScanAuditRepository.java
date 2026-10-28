package com.emilhuseynov.accesspass.scan;

import org.springframework.data.repository.Repository;

public interface ScanAuditRepository extends Repository<ScanAuditEntry, Long>, ScanAuditSearch {

    ScanAuditEntry save(ScanAuditEntry entry);
}
