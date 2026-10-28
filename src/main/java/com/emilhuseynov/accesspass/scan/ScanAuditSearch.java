package com.emilhuseynov.accesspass.scan;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ScanAuditSearch {

    Page<ScanAuditEntry> search(ScanAuditFilter filter, Pageable pageable);
}
