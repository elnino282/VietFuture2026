package org.example.QuanLyMuaVu.module.sustainability.repository;

import java.util.List;
import org.example.QuanLyMuaVu.module.sustainability.entity.IrrigationWaterAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IrrigationWaterAnalysisRepository extends JpaRepository<IrrigationWaterAnalysis, Integer> {

    List<IrrigationWaterAnalysis> findAllBySeasonIdOrderBySampleDateDescCreatedAtDesc(Integer seasonId);

    List<IrrigationWaterAnalysis> findAllBySeason_IdOrderBySampleDateDescCreatedAtDesc(Integer seasonId);

    List<IrrigationWaterAnalysis> findAllBySeasonIdAndPlotIdOrderBySampleDateDescCreatedAtDesc(
            Integer seasonId,
            Integer plotId
    );

    List<IrrigationWaterAnalysis> findAllBySeason_IdAndPlot_IdOrderBySampleDateDescCreatedAtDesc(
            Integer seasonId,
            Integer plotId
    );

    List<IrrigationWaterAnalysis> findAllBySeasonIdAndPlotId(Integer seasonId, Integer plotId);

    List<IrrigationWaterAnalysis> findAllBySeason_IdAndPlot_Id(Integer seasonId, Integer plotId);

    boolean existsByLegacyEventId(Integer legacyEventId);
}
