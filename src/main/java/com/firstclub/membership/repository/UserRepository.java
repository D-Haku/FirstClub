package com.firstclub.membership.repository;

import com.firstclub.membership.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link User}.
 *
 * <p>Provides standard CRUD operations keyed by the caller-assigned
 * {@code String} user identifier.
 *
 * <p>Validates: Requirements 8.5.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
}
