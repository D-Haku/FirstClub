package com.firstclub.membership.repository;

import com.firstclub.membership.domain.SubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link SubscriptionHistory} audit rows.
 *
 * <p>History is read in chronological order via
 * {@link #findBySubscriptionIdOrderByChangedAtAsc(String)} to surface a
 * subscription's tier-change timeline.
 *
 * <p>Validates: Requirements 3.2.
 */
@Repository
public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, Long> {

    /**
     * Returns the audit trail for a subscription in ascending
     * {@code changedAt} order.
     *
     * @param subscriptionId owning subscription identifier
     * @return ordered list of history rows; empty if no changes recorded
     */
    List<SubscriptionHistory> findBySubscriptionIdOrderByChangedAtAsc(String subscriptionId);
}
