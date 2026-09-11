package com.hosannasolutions.solisla.user.repository;

import com.hosannasolutions.solisla.user.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Caller must lowercase {@code email} before calling — emails are normalized to lowercase
     * at write time (see {@link User#User}), not via a query-time function index.
     */
    Optional<User> findByEmail(String email);
}
