package org.example.QuanLyMuaVu.module.farm.repository;

import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FarmRepository extends JpaRepository<Farm, Integer> {

        List<Farm> findAllByUser(org.example.QuanLyMuaVu.module.identity.entity.User user);

        List<Farm> findAllByUser_Id(Long userId);

        Optional<Farm> findByIdAndUser(Integer id, org.example.QuanLyMuaVu.module.identity.entity.User user);

        boolean existsByUserAndNameIgnoreCase(org.example.QuanLyMuaVu.module.identity.entity.User user, String name);

        /**
         * Check if an active farm with the same name exists for the user.
         * Used to prevent duplicate farm names for ACTIVE farms only.
         * This allows users to create a new farm with the same name after soft-deleting
         * the old one.
         */
        boolean existsByUserAndNameIgnoreCaseAndActiveTrue(org.example.QuanLyMuaVu.module.identity.entity.User user, String name);

        boolean existsByUserAndNameIgnoreCaseAndActiveTrueAndIdNot(org.example.QuanLyMuaVu.module.identity.entity.User user, String name, Integer id);

        /**
         * Check if a farm has any plots.
         * More efficient than loading all plots into memory.
         */
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) > 0 FROM Plot p WHERE p.farm.id = :farmId")
        boolean hasPlots(@org.springframework.data.repository.query.Param("farmId") Integer farmId);

        /**
         * Check if a farm has any seasons (through plots).
         * More efficient than loading all plots and seasons into memory.
         */
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) > 0 FROM Season s WHERE s.plot.farm.id = :farmId")
        boolean hasSeasons(@org.springframework.data.repository.query.Param("farmId") Integer farmId);

        /**
         * Find farm by ID only if the user matches.
         * Used for ownership verification.
         * 
         * @param farmId the farm ID
         * @param userId the expected user's ID
         * @return Optional containing the farm if found and owned
         */
        @Query("SELECT f FROM Farm f WHERE f.id = :farmId AND f.user.id = :userId")
        Optional<Farm> findByIdAndUserId(@Param("farmId") Integer farmId, @Param("userId") Long userId);

        /**
         * Check if a farm exists and is owned by the specified user.
         * 
         * @param farmId the farm ID
         * @param userId the expected user's ID
         * @return true if farm exists and is owned by user
         */
        @Query("SELECT COUNT(f) > 0 FROM Farm f WHERE f.id = :farmId AND f.user.id = :userId")
        boolean existsByIdAndUserId(@Param("farmId") Integer farmId, @Param("userId") Long userId);

        /**
         * Count active farms by user ID.
         * Used for dashboard active farms count.
         */
        @Query("SELECT COUNT(f) FROM Farm f WHERE f.user.id = :userId AND f.active = true")
        long countByUserIdAndActiveTrue(@Param("userId") Long userId);

        @Query("SELECT f FROM Farm f WHERE f.user = :user "
                        + "AND (:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
                        + "AND (:active IS NULL OR f.active = :active)")
        Page<Farm> searchByUserAndKeywordAndActive(
                        @Param("user") org.example.QuanLyMuaVu.module.identity.entity.User user,
                        @Param("keyword") String keyword,
                        @Param("active") Boolean active,
                        Pageable pageable);

        // ═══════════════════════════════════════════════════════════════
        // ADMIN QUERY METHODS (global access - no owner filter)
        // ═══════════════════════════════════════════════════════════════

        /**
         * Count all active farms (admin global view).
         */
        @Query("SELECT COUNT(f) FROM Farm f WHERE f.active = true")
        long countByIsActiveTrue();

        /**
         * Count all inactive farms (admin global view).
         */
        @Query("SELECT COUNT(f) FROM Farm f WHERE f.active = false")
        long countByIsActiveFalse();

        /**
         * Search farms by name (admin global view).
         */
        @Query("SELECT f FROM Farm f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<Farm> findByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

        // ═══════════════════════════════════════════════════════════════
        // LEFT JOIN FETCH QUERIES (to handle missing org.example.QuanLyMuaVu.module.identity.entity.User references)
        // ═══════════════════════════════════════════════════════════════

        /**
         * Fetch all farms with LEFT JOIN FETCH on user.
         * This ensures missing org.example.QuanLyMuaVu.module.identity.entity.User references return NULL instead of
         * EntityNotFoundException.
         * The countQuery avoids JOIN FETCH which is not allowed in count queries.
         */
        @Query(value = "SELECT f FROM Farm f LEFT JOIN FETCH f.user LEFT JOIN FETCH f.province LEFT JOIN FETCH f.ward", countQuery = "SELECT COUNT(f) FROM Farm f")
        Page<Farm> findAllWithRelationships(Pageable pageable);

        /**
         * Search farms by name with LEFT JOIN FETCH on user.
         * This ensures missing org.example.QuanLyMuaVu.module.identity.entity.User references return NULL instead of
         * EntityNotFoundException.
         * The countQuery avoids JOIN FETCH which is not allowed in count queries.
         */
        @Query(value = "SELECT f FROM Farm f LEFT JOIN FETCH f.user LEFT JOIN FETCH f.province LEFT JOIN FETCH f.ward "
                        +
                        "WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))", countQuery = "SELECT COUNT(f) FROM Farm f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<Farm> findByNameContainingIgnoreCaseWithRelationships(@Param("keyword") String keyword, Pageable pageable);

        /**
         * Check if a user owns any farms.
         * Used for admin user deletion validation.
         */
        @Query("SELECT COUNT(f) > 0 FROM Farm f WHERE f.user.id = :userId")
        boolean existsByUserId(@Param("userId") Long userId);
}
