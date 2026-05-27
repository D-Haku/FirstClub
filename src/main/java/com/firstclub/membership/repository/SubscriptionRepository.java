package com.firstclub.membership.repository;

import com.firstclub.membership.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Subscription}.
 *
 * <p>Exposes lookups for the current active subscription of a user and
 * for previously-stored idempotency keys, supporting at-least-once
 * subscribe semantics.
 *
 * <p>Validates: Requirements 2.3, 2.6, 4.3, 5.1, 5.3.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    /**
     * Finds the user's currently-active subscription, defined as
     * {@code status = ACTIVE} and {@code endAt} strictly after {@code now}.
     *
     * @param userId owning user identifier
     * @param now    evaluation instant supplied by the caller's clock
     * @return the active subscription, or {@link Optional#empty()} if none
     */
    @Query("SELECT s FROM Subscription s "
            + "WHERE s.userId = :userId "
            + "AND s.status = com.firstclub.membership.domain.SubscriptionStatus.ACTIVE "
            + "AND s.endAt > :now")
    Optional<Subscription> findActive(@Param("userId") String userId,
                                      @Param("now") Instant now);

    /**
     * Looks up a subscription previously written with the given idempotency
     * key, allowing the subscribe flow to return the original result on
     * retried requests instead of creating a duplicate row.
     *
     * @param idempotencyKey client-supplied idempotency key
     * @return matching subscription, or {@link Optional#empty()} if none
     */
    Optional<Subscription> findByIdempotencyKey(String idempotencyKey);
}
