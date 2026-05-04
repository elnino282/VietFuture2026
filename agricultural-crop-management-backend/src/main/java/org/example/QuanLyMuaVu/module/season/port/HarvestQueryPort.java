package org.example.QuanLyMuaVu.module.season.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.season.entity.Harvest;

public interface HarvestQueryPort {

    BigDecimal sumQuantityBySeasonId(Integer seasonId);

    BigDecimal sumRevenueBySeasonId(Integer seasonId);

    Optional<Harvest> findHarvestById(Integer harvestId);

    boolean existsHarvestBySeasonId(Integer seasonId);

    List<Harvest> findAllHarvestsBySeasonId(Integer seasonId);

    List<Harvest> findAllHarvestsBySeasonIds(Iterable<Integer> seasonIds);
}
