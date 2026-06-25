package org.example.farm.repository;

import java.util.List;
import java.util.Optional;
import org.example.farm.entity.Farm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FarmRepository extends JpaRepository<Farm, Integer> {

    List<Farm> findAllByUserId(Long userId);

    Optional<Farm> findByIdAndUserId(Integer id, Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndActiveTrue(Long userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndActiveTrueAndIdNot(Long userId, String name, Integer id);

    @Query("SELECT COUNT(p) > 0 FROM Plot p WHERE p.farm.id = :farmId")
    boolean hasPlots(@Param("farmId") Integer farmId);

    @Query("SELECT f FROM Farm f " +
           "LEFT JOIN FETCH f.province " +
           "LEFT JOIN FETCH f.ward " +
           "WHERE f.active = true AND f.userId IN :userIds " +
           "ORDER BY f.userId ASC, f.id ASC")
    List<Farm> findActiveByUserIds(@Param("userIds") List<Long> userIds);

    boolean existsByIdAndUserId(Integer farmId, Long userId);

    long countByUserIdAndActiveTrue(Long userId);

    @Query("SELECT f FROM Farm f WHERE f.userId = :userId "
            + "AND (:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
            + "AND (:active IS NULL OR f.active = :active)")
    Page<Farm> searchByUserIdAndKeywordAndActive(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("SELECT COUNT(f) FROM Farm f WHERE f.active = true")
    long countByIsActiveTrue();

    @Query("SELECT COUNT(f) FROM Farm f WHERE f.active = false")
    long countByIsActiveFalse();

    @Query("SELECT f FROM Farm f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Farm> findByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT f FROM Farm f LEFT JOIN FETCH f.province LEFT JOIN FETCH f.ward", countQuery = "SELECT COUNT(f) FROM Farm f")
    Page<Farm> findAllWithRelationships(Pageable pageable);

    @Query(value = "SELECT f FROM Farm f LEFT JOIN FETCH f.province LEFT JOIN FETCH f.ward "
            + "WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))", countQuery = "SELECT COUNT(f) FROM Farm f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Farm> findByNameContainingIgnoreCaseWithRelationships(@Param("keyword") String keyword, Pageable pageable);

    boolean existsByUserId(Long userId);
}
