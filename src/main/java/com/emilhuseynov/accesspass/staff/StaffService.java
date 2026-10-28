package com.emilhuseynov.accesspass.staff;

import com.emilhuseynov.accesspass.common.ConflictException;
import com.emilhuseynov.accesspass.common.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffService {

    private final StaffAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String unknownUserHash;

    public StaffService(StaffAccountRepository accounts, PasswordEncoder passwordEncoder, Clock clock) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.unknownUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public StaffAccount create(String username, String rawPassword, StaffRole role) {
        if (accounts.existsByUsername(username)) {
            throw new ConflictException("Username '" + username + "' is already taken");
        }
        return accounts.save(new StaffAccount(username, passwordEncoder.encode(rawPassword), role, clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<StaffAccount> list() {
        return accounts.findAllByOrderByUsername();
    }

    @Transactional
    public StaffAccount disable(Long id) {
        StaffAccount account = accounts.findById(id).orElseThrow(() -> new NotFoundException("Staff account", id));
        account.disable();
        return account;
    }

    @Transactional(readOnly = true)
    public StaffAccount authenticate(String username, String rawPassword) {
        StaffAccount account = accounts.findByUsername(username).orElse(null);
        if (account == null) {
            passwordEncoder.matches(rawPassword, unknownUserHash);
            throw new BadCredentialsException("Invalid username or password");
        }
        if (!passwordEncoder.matches(rawPassword, account.getPasswordHash()) || !account.isEnabled()) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return account;
    }
}
