package org.example.adminreporting.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.example.adminreporting.entity.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogEntryRepository extends JpaRepository<AuditLogEntry, Long> {

    List<AuditLogEntry> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(String entityType, Integer entityId);

    @Query("""
            SELECT a
            FROM AuditLogEntry a
            WHERE (:fromTime IS NULL OR a.performedAt >= :fromTime)
              AND (:toTime IS NULL OR a.performedAt <= :toTime)
              AND (:module IS NULL OR UPPER(a.entityType) = UPPER(:module)
                   OR LOCATE(CONCAT(UPPER(:module), '_'), UPPER(a.entityType)) = 1)
              AND (:entityType IS NULL OR UPPER(a.entityType) = UPPER(:entityType))
              AND (:operation IS NULL OR UPPER(a.operation) = UPPER(:operation))
              AND (:performedBy IS NULL OR LOWER(a.performedBy) LIKE LOWER(CONCAT('%', :performedBy, '%')))
              AND (:entityId IS NULL OR a.entityId = :entityId)
            """)
    Page<AuditLogEntry> searchAuditLogs(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("module") String module,
            @Param("entityType") String entityType,
            @Param("operation") String operation,
            @Param("performedBy") String performedBy,
            @Param("entityId") Integer entityId,
            Pageable pageable);
}
