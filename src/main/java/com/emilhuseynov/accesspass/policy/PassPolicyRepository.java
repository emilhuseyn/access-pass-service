package com.emilhuseynov.accesspass.policy;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassPolicyRepository extends JpaRepository<PassPolicy, Long> {

    @EntityGraph(attributePaths = {"site", "gates"})
    Optional<PassPolicy> findWithGatesById(Long id);

    @EntityGraph(attributePaths = {"site", "gates"})
    List<PassPolicy> findBySiteIdOrderByName(Long siteId);

    boolean existsBySiteIdAndNameIgnoreCase(Long siteId, String name);
}
