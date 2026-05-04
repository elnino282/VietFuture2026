package org.example.QuanLyMuaVu.module.admin.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.example.QuanLyMuaVu.module.admin.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit logs for a specific entity, ordered by most recent first.
     * Useful for viewing audit trail of a specific farm, plot, season, etc.
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(
            String entityType,
            Integer entityId);

    /**
     * Find audit logs by operation type (e.g., all SOFT_DELETE operations).
     */
    List<AuditLog> findByOperationOrderByPerformedAtDesc(String operation);

    @Query("""
            SELECT a
            FROM AuditLog a
            WHERE (:fromTime IS NULL OR a.performedAt >= :fromTime)
              AND (:toTime IS NULL OR a.performedAt <= :toTime)
              AND (:module IS NULL OR UPPER(a.entityType) = UPPER(:module)
                   OR LOCATE(CONCAT(UPPER(:module), '_'), UPPER(a.entityType)) = 1)
              AND (:entityType IS NULL OR UPPER(a.entityType) = UPPER(:entityType))
              AND (:operation IS NULL OR UPPER(a.operation) = UPPER(:operation))
              AND (:performedBy IS NULL OR LOWER(a.performedBy) LIKE LOWER(CONCAT('%', :performedBy, '%')))
              AND (:entityId IS NULL OR a.entityId = :entityId)
            """)
    Page<AuditLog> searchAuditLogs(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("module") String module,
            @Param("entityType") String entityType,
            @Param("operation") String operation,
            @Param("performedBy") String performedBy,
            @Param("entityId") Integer entityId,
            Pageable pageable);
}
