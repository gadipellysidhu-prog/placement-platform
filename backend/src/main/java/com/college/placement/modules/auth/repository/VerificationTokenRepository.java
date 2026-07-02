package com.college.placement.modules.auth.repository;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.VerificationToken;
import com.college.placement.modules.auth.domain.VerificationTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByTokenHash(String tokenHash);

    /**
     * Atomically consumes a token. Returns the number of rows updated: 1 on the
     * first (winning) call, 0 if the token was already consumed/revoked — giving
     * race-free, one-time consumption under concurrent requests.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VerificationToken t SET t.consumed = true, t.consumedAt = :now "
            + "WHERE t.id = :id AND t.consumed = false AND t.revoked = false")
    int markConsumed(@Param("id") UUID id, @Param("now") Instant now);

    /** Revokes every still-active token of a type for a user (only-latest-valid). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VerificationToken t SET t.revoked = true "
            + "WHERE t.user = :user AND t.type = :type AND t.consumed = false AND t.revoked = false")
    void revokeActive(@Param("user") AppUser user, @Param("type") VerificationTokenType type);

    /** Cleanup: removes expired tokens and any already consumed or revoked. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM VerificationToken t "
            + "WHERE t.expiresAt < :now OR t.consumed = true OR t.revoked = true")
    int deleteExpiredAndUsed(@Param("now") Instant now);
}
