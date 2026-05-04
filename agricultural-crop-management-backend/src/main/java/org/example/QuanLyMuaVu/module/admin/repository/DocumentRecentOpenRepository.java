package org.example.QuanLyMuaVu.module.admin.repository;

import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.admin.entity.DocumentRecentOpen;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRecentOpenRepository extends JpaRepository<DocumentRecentOpen, Integer> {

    /**
     * Find recent opens for a user, ordered by most recent first
     */
    List<DocumentRecentOpen> findByUserIdOrderByOpenedAtDesc(Long userId, Pageable pageable);

    /**
     * Find existing record for user+document (to update timestamp)
     */
    Optional<DocumentRecentOpen> findByUserIdAndDocumentId(Long userId, Integer documentId);

    /**
     * Get recent document IDs for user
     */
    @Query("SELECT r.documentId FROM DocumentRecentOpen r WHERE r.userId = :userId ORDER BY r.openedAt DESC")
    List<Integer> findRecentDocumentIdsByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Delete all recent opens for a specific user.
     * Used when deleting a user to clean up related records.
     */
    @Modifying
    @Query("DELETE FROM DocumentRecentOpen r WHERE r.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
