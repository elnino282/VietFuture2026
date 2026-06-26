package org.example.sustainability.snapshot.repository;

import java.util.List;
import org.example.sustainability.snapshot.entity.FarmSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FarmSnapshotRepository extends JpaRepository<FarmSnapshot, Integer> {

    @Query(value = """
            SELECT fs.* FROM farm_snapshots fs
            INNER JOIN (
                SELECT farm_id, MAX(snapshot_at) AS max_at FROM farm_snapshots WHERE farm_id = :farmId GROUP BY farm_id
            ) latest ON fs.farm_id = latest.farm_id AND fs.snapshot_at = latest.max_at
            WHERE fs.farm_id = :farmId AND (fs.active IS NULL OR fs.active = true)
            LIMIT 1
            """, nativeQuery = true)
    FarmSnapshot findLatestByFarmId(@Param("farmId") Integer farmId);

    @Query(value = """
            SELECT fs.* FROM farm_snapshots fs
            INNER JOIN (
                SELECT farm_id, MAX(snapshot_at) AS max_at FROM farm_snapshots
                WHERE user_id = :userId GROUP BY farm_id
            ) latest ON fs.farm_id = latest.farm_id AND fs.snapshot_at = latest.max_at
            WHERE fs.user_id = :userId AND (fs.active IS NULL OR fs.active = true)
            """, nativeQuery = true)
    List<FarmSnapshot> findLatestFarmsForUser(@Param("userId") Long userId);
}
