package org.example.QuanLyMuaVu.module.admin.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.example.QuanLyMuaVu.module.admin.entity.DocumentFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentFavoriteRepository extends JpaRepository<DocumentFavorite, Integer> {

    /**
     * Find all favorites for a user
     */
    List<DocumentFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find specific favorite
     */
    Optional<DocumentFavorite> findByUserIdAndDocumentId(Long userId, Integer documentId);

    /**
     * Delete a favorite
     */
    void deleteByUserIdAndDocumentId(Long userId, Integer documentId);

    /**
     * Check if user has favorited a document
     */
    boolean existsByUserIdAndDocumentId(Long userId, Integer documentId);

    /**
     * Get all document IDs favorited by user
     */
    @Query("SELECT f.documentId FROM DocumentFavorite f WHERE f.userId = :userId")
    Set<Integer> findDocumentIdsByUserId(@Param("userId") Long userId);
    
    /**
     * Delete all favorites for a specific user.
     * Used when deleting a user to clean up related records.
     */
    @Modifying
    @Query("DELETE FROM DocumentFavorite f WHERE f.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
