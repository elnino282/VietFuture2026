package org.example.QuanLyMuaVu.module.season.service;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.module.season.entity.Harvest;
import org.example.QuanLyMuaVu.module.season.port.HarvestQueryPort;
import org.example.QuanLyMuaVu.module.season.repository.HarvestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class HarvestQueryService implements HarvestQueryPort {

    HarvestRepository harvestRepository;

    @Override
    public BigDecimal sumQuantityBySeasonId(Integer seasonId) {
        if (seasonId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = harvestRepository.sumQuantityBySeasonId(seasonId);
        return value != null ? value : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal sumRevenueBySeasonId(Integer seasonId) {
        if (seasonId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = harvestRepository.sumRevenueBySeasonId(seasonId);
        return value != null ? value : BigDecimal.ZERO;
    }

    @Override
    public Optional<Harvest> findHarvestById(Integer harvestId) {
        if (harvestId == null) {
            return Optional.empty();
        }
        return harvestRepository.findById(harvestId);
    }

    @Override
    public boolean existsHarvestBySeasonId(Integer seasonId) {
        if (seasonId == null) {
            return false;
        }
        return harvestRepository.existsBySeason_Id(seasonId);
    }

    @Override
    public List<Harvest> findAllHarvestsBySeasonId(Integer seasonId) {
        if (seasonId == null) {
            return List.of();
        }
        return harvestRepository.findAllBySeason_Id(seasonId);
    }

    @Override
    public List<Harvest> findAllHarvestsBySeasonIds(Iterable<Integer> seasonIds) {
        if (seasonIds == null) {
            return List.of();
        }
        return harvestRepository.findAllBySeason_IdIn(seasonIds);
    }
}
