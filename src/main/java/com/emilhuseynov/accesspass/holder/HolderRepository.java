package com.emilhuseynov.accesspass.holder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolderRepository extends JpaRepository<Holder, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByExternalRef(String externalRef);

    Page<Holder> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);
}
