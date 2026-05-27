package com.firstclub.membership.repository;

import com.firstclub.membership.domain.IdempotencyId;
import com.firstclub.membership.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link IdempotencyRecord}.
 *
 * <p>The composite key is modeled by {@link IdempotencyId}; the derived
 * lookup below mirrors that triplet so the service layer can fetch a
 * stored response without constructing an {@code IdempotencyId} value.
 *
 * <p>Validates: Requirements 2.4, 8.3, 9.4.
 */
@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, IdempotencyId> {

    /**
     * Finds the stored idempotency record for the given triplet.
     *
     * @param userId    owning user identifier
     * @param key       client-supplied idempotency key
     * @param operation logical operation name (e.g., "subscribe", "checkout")
     * @return the matching record, or {@link Optional#empty()} if none
     */
    Optional<IdempotencyRecord> findByUserIdAndKeyAndOperation(String userId,
                                                               String key,
                                                               String operation);
}
