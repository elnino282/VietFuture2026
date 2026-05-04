package org.example.QuanLyMuaVu.module.season.port;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.module.season.entity.Season;

public interface SeasonQueryPort {

    Optional<Season> findSeasonById(Integer seasonId);

    List<Season> findAllSeasonsByOwnerId(Long ownerId);

    List<Season> findActiveSeasonsByOwnerIdOrderByStartDateDesc(Long ownerId);

    List<Season> findAllSeasonsByPlotId(Integer plotId);

    List<Season> findAllSeasonsByFarmIds(Iterable<Integer> farmIds);

    List<Season> findAllSeasonsByPlotIdOrderByStartDateDesc(Integer plotId);

    List<Season> findAllSeasonsByPlotIdOrderByStartDateAsc(Integer plotId);

    List<Season> findAllSeasonsByFilters(
            LocalDate from,
            LocalDate to,
            Integer cropId,
            Integer farmId,
            Integer plotId,
            Integer varietyId);

    List<Season> findAllSeasons();

    long countSeasonsByStatusAndOwnerId(SeasonStatus status, Long ownerId);

    long countSeasons();

    boolean existsSeasonByVarietyId(Integer varietyId);

    long countFieldLogsBySeasonAndLogType(Integer seasonId, String logType);
}
