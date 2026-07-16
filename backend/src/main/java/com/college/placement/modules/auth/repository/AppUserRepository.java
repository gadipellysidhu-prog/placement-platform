package com.college.placement.modules.auth.repository;

import com.college.placement.modules.auth.domain.AccountStatus;
import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Count for last-administrator protection. */
    long countByRoleAndStatus(Role role, AccountStatus status);

    /**
     * Admin listing with optional role/status/email filters (any may be null).
     *
     * <p>{@code :query} is cast explicitly because it is nullable: with a null bind and no
     * cast, PostgreSQL cannot infer the parameter's type, defaults it to {@code bytea} and
     * fails the whole statement with "function lower(bytea) does not exist". The cast is
     * inert on H2, which is why the test suite never saw it.
     */
    @Query("SELECT u FROM AppUser u WHERE (:role IS NULL OR u.role = :role) "
            + "AND (:status IS NULL OR u.status = :status) "
            + "AND (:query IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:query AS String), '%')))")
    Page<AppUser> search(@Param("role") Role role,
                         @Param("status") AccountStatus status,
                         @Param("query") String query,
                         Pageable pageable);
}
