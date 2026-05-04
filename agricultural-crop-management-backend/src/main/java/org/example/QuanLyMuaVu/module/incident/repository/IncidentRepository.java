package org.example.QuanLyMuaVu.module.incident.repository;

import java.util.List;
import org.example.QuanLyMuaVu.Enums.IncidentStatus;
import org.example.QuanLyMuaVu.module.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Integer>, JpaSpecificationExecutor<Incident> {

    List<Incident> findAllBySeason(org.example.QuanLyMuaVu.module.season.entity.Season season);

    List<Incident> findAllBySeasonId(Integer seasonId);

    List<Incident> findAllBySeason_Id(Integer seasonId);

    long countBySeasonIdAndStatusIn(Integer seasonId, List<IncidentStatus> statuses);

    long countBySeason_IdAndStatusIn(Integer seasonId, List<IncidentStatus> statuses);

    /**
     * Count incidents by season and status for summary chips
     */
    long countBySeasonIdAndStatus(Integer seasonId, IncidentStatus status);

    /**
     * Count incidents by season and status for summary chips.
     * Legacy object-based method retained for compatibility.
     */
    long countBySeasonAndStatus(org.example.QuanLyMuaVu.module.season.entity.Season season, IncidentStatus status);

    /**
     * Find all seasons that have incidents reported by a specific user
     */
    @Query("SELECT DISTINCT i.season FROM Incident i WHERE i.reportedBy = :user")
    List<org.example.QuanLyMuaVu.module.season.entity.Season> findDistinctSeasonsByReportedBy(@Param("user") org.example.QuanLyMuaVu.module.identity.entity.User user);

    /**
     * Count open incidents for farmer's seasons.
     * Used for dashboard alert count.
     */
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.season.plot.farm.user.id = :ownerId AND i.status IN :openStatuses")
    long countByFarmUserIdAndStatusIn(@Param("ownerId") Long ownerId,
            @Param("openStatuses") List<IncidentStatus> openStatuses);

    // ═══════════════════════════════════════════════════════════════
    // ADMIN QUERY METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Count incidents by status list (admin dashboard).
     */
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<String> statuses);
}
