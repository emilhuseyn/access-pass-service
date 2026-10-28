package com.emilhuseynov.accesspass.staff;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAccountRepository extends JpaRepository<StaffAccount, Long> {

    Optional<StaffAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    List<StaffAccount> findAllByOrderByUsername();
}
