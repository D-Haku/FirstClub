package com.firstclub.membership.repository;

import com.firstclub.membership.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Spring Data JPA repository for {@link Order}.
 *
 * <p>Aggregations exposed here back the tier-recommendation subsystem,
 * which evaluates trailing order count and trailing spend per user.
 *
 * <p>Validates: Requirements 6.1, 6.5.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * Counts orders placed by a user strictly after the given instant.
     *
     * @param userId owning user identifier
     * @param since  exclusive lower bound on {@code placedAt}
     * @return count of matching orders
     */
    long countByUserIdAndPlacedAtAfter(String userId, Instant since);

    /**
     * Sums the {@code total} of orders placed by a user strictly after the
     * given instant. Returns {@link BigDecimal#ZERO} when no orders match,
     * via {@code COALESCE}, so callers never receive {@code null}.
     *
     * @param userId owning user identifier
     * @param since  exclusive lower bound on {@code placedAt}
     * @return summed total of matching orders, or {@code 0} if none
     */
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o "
            + "WHERE o.userId = :userId AND o.placedAt > :since")
    BigDecimal sumTotalByUserIdAndPlacedAtAfter(@Param("userId") String userId,
                                                @Param("since") Instant since);
}
