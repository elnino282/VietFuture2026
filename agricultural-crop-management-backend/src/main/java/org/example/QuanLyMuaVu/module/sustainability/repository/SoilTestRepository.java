package org.example.QuanLyMuaVu.module.sustainability.repository;

import java.util.List;
import org.example.QuanLyMuaVu.module.sustainability.entity.SoilTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SoilTestRepository extends JpaRepository<SoilTest, Integer> {

    List<SoilTest> findAllBySeasonIdOrderBySampleDateDescCreatedAtDesc(Integer seasonId);

    List<SoilTest> findAllBySeason_IdOrderBySampleDateDescCreatedAtDesc(Integer seasonId);

    List<SoilTest> findAllBySeasonIdAndPlotIdOrderBySampleDateDescCreatedAtDesc(
            Integer seasonId,
            Integer plotId
    );

    List<SoilTest> findAllBySeason_IdAndPlot_IdOrderBySampleDateDescCreatedAtDesc(
            Integer seasonId,
            Integer plotId
    );

    List<SoilTest> findAllBySeasonIdAndPlotId(Integer seasonId, Integer plotId);

    List<SoilTest> findAllBySeason_IdAndPlot_Id(Integer seasonId, Integer plotId);

    boolean existsByLegacyEventId(Integer legacyEventId);
}
