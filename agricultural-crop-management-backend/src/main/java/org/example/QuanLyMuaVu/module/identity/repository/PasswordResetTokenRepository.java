package org.example.QuanLyMuaVu.module.identity.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.identity.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
            String tokenHash,
            LocalDateTime now);

    // Debug query to check if token exists regardless of expiry/used status
    Optional<PasswordResetToken> findFirstByTokenHash(String tokenHash);
    
    /**
     * Delete all password reset tokens for a specific user.
     * Used when deleting a user to clean up related records.
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
