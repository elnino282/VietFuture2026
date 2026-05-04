package org.example.QuanLyMuaVu.module.incident.repository;



import java.time.LocalDateTime;
import org.example.QuanLyMuaVu.module.incident.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    @Query("""
            SELECT a FROM Alert a
            WHERE (:type IS NULL OR UPPER(a.type) = UPPER(:type))
              AND (:severity IS NULL OR UPPER(a.severity) = UPPER(:severity))
              AND (:status IS NULL OR UPPER(a.status) = UPPER(:status))
              AND (:farmId IS NULL OR a.farmId = :farmId)
              AND (:fromDate IS NULL OR a.createdAt >= :fromDate)
            """)
    Page<Alert> search(
            @Param("type") String type,
            @Param("severity") String severity,
            @Param("status") String status,
            @Param("farmId") Integer farmId,
            @Param("fromDate") LocalDateTime fromDate,
            Pageable pageable);
}
