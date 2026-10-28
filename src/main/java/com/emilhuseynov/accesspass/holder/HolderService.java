package com.emilhuseynov.accesspass.holder;

import com.emilhuseynov.accesspass.common.ConflictException;
import com.emilhuseynov.accesspass.common.NotFoundException;
import java.time.Clock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HolderService {

    private final HolderRepository holders;
    private final Clock clock;

    public HolderService(HolderRepository holders, Clock clock) {
        this.holders = holders;
        this.clock = clock;
    }

    @Transactional
    public Holder create(String fullName, String email, String externalRef) {
        if (email != null && holders.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A holder with this email already exists");
        }
        if (externalRef != null && holders.existsByExternalRef(externalRef)) {
            throw new ConflictException("A holder with external reference '" + externalRef + "' already exists");
        }
        return holders.save(new Holder(fullName, email, externalRef, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Holder get(Long id) {
        return holders.findById(id).orElseThrow(() -> new NotFoundException("Holder", id));
    }

    @Transactional(readOnly = true)
    public Page<Holder> search(String name, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return holders.findAll(pageable);
        }
        return holders.findByFullNameContainingIgnoreCase(name.strip(), pageable);
    }

    @Transactional
    public Holder deactivate(Long id) {
        Holder holder = get(id);
        holder.deactivate();
        return holder;
    }

    @Transactional
    public Holder activate(Long id) {
        Holder holder = get(id);
        holder.activate();
        return holder;
    }
}
