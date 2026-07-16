package com.college.placement.modules.auth.repository;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Most recent refresh-token issuance per user — a token is minted on every login
     * and every refresh, so this is the newest record of account activity available
     * without introducing new state. Returns {@code [userId, lastActivityAt]} rows,
     * grouped in the database; users with no surviving token are simply absent.
     *
     * <p>Necessarily lossy: {@code deleteExpiredAndRevoked} purges expired/revoked
     * tokens, so a long-dormant (or disabled) user reports no activity rather than a
     * stale timestamp. Absent is reported as unknown, never as a fabricated date.
     */
    @Query("SELECT t.user.id, MAX(t.createdAt) FROM RefreshToken t "
            + "WHERE t.user.id IN :userIds GROUP BY t.user.id")
    List<Object[]> findLastActivityByUserIds(@Param("userIds") Collection<UUID> userIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user = :user AND t.revoked = false")
    void revokeAllByUser(AppUser user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now OR t.revoked = true")
    void deleteExpiredAndRevoked(Instant now);
}
