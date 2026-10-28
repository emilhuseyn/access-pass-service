package com.emilhuseynov.accesspass.scan;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.transaction.annotation.Transactional;

class ScanAuditSearchImpl implements ScanAuditSearch {

    private final EntityManager entityManager;

    ScanAuditSearchImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScanAuditEntry> search(ScanAuditFilter filter, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<ScanAuditEntry> query = cb.createQuery(ScanAuditEntry.class);
        Root<ScanAuditEntry> root = query.from(ScanAuditEntry.class);
        query.where(predicates(cb, root, filter))
                .orderBy(cb.desc(root.get("occurredAt")), cb.desc(root.get("id")));
        List<ScanAuditEntry> content = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> count = cb.createQuery(Long.class);
        Root<ScanAuditEntry> countRoot = count.from(ScanAuditEntry.class);
        count.select(cb.count(countRoot)).where(predicates(cb, countRoot, filter));

        return PageableExecutionUtils.getPage(content, pageable,
                () -> entityManager.createQuery(count).getSingleResult());
    }

    private static Predicate[] predicates(CriteriaBuilder cb, Root<ScanAuditEntry> root, ScanAuditFilter filter) {
        List<Predicate> predicates = new ArrayList<>();
        if (filter.gateId() != null) {
            predicates.add(cb.equal(root.get("gateId"), filter.gateId()));
        }
        if (filter.deviceId() != null) {
            predicates.add(cb.equal(root.get("deviceId"), filter.deviceId()));
        }
        if (filter.passId() != null) {
            predicates.add(cb.equal(root.get("passId"), filter.passId()));
        }
        if (filter.holderId() != null) {
            predicates.add(cb.equal(root.get("holderId"), filter.holderId()));
        }
        if (filter.outcome() != null) {
            predicates.add(cb.equal(root.get("outcome"), filter.outcome()));
        }
        if (filter.reason() != null) {
            predicates.add(cb.equal(root.get("reason"), filter.reason()));
        }
        if (filter.from() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), filter.from()));
        }
        if (filter.to() != null) {
            predicates.add(cb.lessThan(root.get("occurredAt"), filter.to()));
        }
        return predicates.toArray(Predicate[]::new);
    }
}
