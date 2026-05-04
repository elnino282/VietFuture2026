package org.example.QuanLyMuaVu.module.sustainability.repository;

import java.util.List;
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.example.QuanLyMuaVu.module.sustainability.entity.NutrientInputEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NutrientInputEventRepository extends JpaRepository<NutrientInputEvent, Integer> {

    List<NutrientInputEvent> findAllBySeasonId(Integer seasonId);

    List<NutrientInputEvent> findAllBySeason_Id(Integer seasonId);

    List<NutrientInputEvent> findAllBySeasonIdAndPlotId(Integer seasonId, Integer plotId);

    List<NutrientInputEvent> findAllBySeason_IdAndPlot_Id(Integer seasonId, Integer plotId);

    List<NutrientInputEvent> findAllByPlotId(Integer plotId);

    List<NutrientInputEvent> findAllByPlot_Id(Integer plotId);

    List<NutrientInputEvent> findAllBySeasonIdAndPlotIdAndInputSource(
            Integer seasonId,
            Integer plotId,
            NutrientInputSource inputSource
    );

    List<NutrientInputEvent> findAllBySeason_IdAndPlot_IdAndInputSource(
            Integer seasonId,
            Integer plotId,
            NutrientInputSource inputSource
    );

    List<NutrientInputEvent> findAllByInputSourceInOrderByIdAsc(List<NutrientInputSource> sources);

    List<NutrientInputEvent> findAllByInputSourceInAndSeasonIdOrderByIdAsc(
            List<NutrientInputSource> sources,
            Integer seasonId
    );

    List<NutrientInputEvent> findAllByInputSourceInAndSeason_IdOrderByIdAsc(
            List<NutrientInputSource> sources,
            Integer seasonId
    );
}
