package com.emilhuseynov.accesspass.pass;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccessPassRepository extends JpaRepository<AccessPass, UUID> {

    @EntityGraph(attributePaths = {"holder", "policy", "policy.site", "policy.gates"})
    Optional<AccessPass> findDetailedById(UUID id);

    @EntityGraph(attributePaths = {"holder", "policy"})
    Page<AccessPass> findByHolderId(Long holderId, Pageable pageable);

    /**
     * Consumes one use in a single conditional UPDATE. The row lock taken by the update serialises concurrent
     * scans of the same pass, and the re-checked predicate makes the losing transaction match zero rows.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AccessPass p
               set p.useCount = p.useCount + 1, p.version = p.version + 1
             where p.id = :id
               and p.status = :active
               and (p.maxUses is null or p.useCount < p.maxUses)
            """)
    int consumeUse(@Param("id") UUID id, @Param("active") PassStatus active);
}
