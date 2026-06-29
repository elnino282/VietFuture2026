package org.example.sustainability.snapshot.repository;

import java.util.List;
import org.example.sustainability.snapshot.entity.SeasonSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonSnapshotRepository extends JpaRepository<SeasonSnapshot, Integer> {

    @Query(value = """
            SELECT ss.* FROM season_snapshots ss
            INNER JOIN (
                SELECT season_id, MAX(snapshot_at) AS max_at FROM season_snapshots WHERE season_id = :seasonId GROUP BY season_id
            ) latest ON ss.season_id = latest.season_id AND ss.snapshot_at = latest.max_at
            WHERE ss.season_id = :seasonId
            LIMIT 1
            """, nativeQuery = true)
    SeasonSnapshot findLatestBySeasonId(@Param("seasonId") Integer seasonId);

    @Query(value = """
            SELECT ss.* FROM season_snapshots ss
            INNER JOIN (
                SELECT season_id, MAX(snapshot_at) AS max_at FROM season_snapshots
                WHERE plot_id = :plotId GROUP BY season_id
            ) latest ON ss.season_id = latest.season_id AND ss.snapshot_at = latest.max_at
            WHERE ss.plot_id = :plotId
            ORDER BY ss.start_date DESC
            """, nativeQuery = true)
    List<SeasonSnapshot> findLatestSeasonsByPlotIdOrderByStartDateDesc(@Param("plotId") Integer plotId);

    @Query(value = """
            SELECT ss.* FROM season_snapshots ss
            INNER JOIN (
                SELECT season_id, MAX(snapshot_at) AS max_at FROM season_snapshots
                WHERE plot_id = :plotId GROUP BY season_id
            ) latest ON ss.season_id = latest.season_id AND ss.snapshot_at = latest.max_at
            WHERE ss.plot_id = :plotId
            ORDER BY ss.start_date ASC
            """, nativeQuery = true)
    List<SeasonSnapshot> findLatestSeasonsByPlotIdOrderByStartDateAsc(@Param("plotId") Integer plotId);

    @Query(value = """
            SELECT ss.* FROM season_snapshots ss
            INNER JOIN (
                SELECT season_id, MAX(snapshot_at) AS max_at FROM season_snapshots GROUP BY season_id
            ) latest ON ss.season_id = latest.season_id AND ss.snapshot_at = latest.max_at
            WHERE ss.status = 'ACTIVE'
            AND ss.farm_id IN (
                SELECT farm_id FROM farm_snapshots fs
                INNER JOIN (
                    SELECT farm_id, MAX(snapshot_at) AS max_at FROM farm_snapshots WHERE user_id = :userId GROUP BY farm_id
                ) f ON fs.farm_id = f.farm_id AND fs.snapshot_at = f.max_at
                WHERE fs.user_id = :userId
            )
            ORDER BY ss.start_date DESC
            """, nativeQuery = true)
    List<SeasonSnapshot> findActiveLatestSeasonsForUser(@Param("userId") Long userId);

    @Query(value = "SELECT COALESCE(SUM(oi.line_total), 0) FROM marketplace_db.marketplace_order_items oi JOIN marketplace_db.marketplace_orders o ON o.id = oi.order_id WHERE oi.season_id = :seasonId AND o.status = 'COMPLETED'", nativeQuery = true)
    java.math.BigDecimal getMarketplaceRevenueBySeasonId(@Param("seasonId") Integer seasonId);

    @Query(value = "SELECT COUNT(*) FROM marketplace_db.marketplace_orders", nativeQuery = true)
    long countMarketplaceOrders();
}
