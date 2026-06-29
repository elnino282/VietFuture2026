package org.example.sustainability.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.sustainability.snapshot.entity.CropSnapshot;
import org.example.sustainability.snapshot.entity.ExpenseSnapshot;
import org.example.sustainability.snapshot.entity.FarmSnapshot;
import org.example.sustainability.snapshot.entity.PlotSnapshot;
import org.example.sustainability.snapshot.entity.SeasonSnapshot;
import org.example.sustainability.snapshot.model.FarmContext;
import org.example.sustainability.snapshot.model.PlotContext;
import org.example.sustainability.snapshot.model.SeasonContext;
import org.example.sustainability.snapshot.repository.CropSnapshotRepository;
import org.example.sustainability.snapshot.repository.ExpenseSnapshotRepository;
import org.example.sustainability.snapshot.repository.FarmSnapshotRepository;
import org.example.sustainability.snapshot.repository.HarvestSnapshotRepository;
import org.example.sustainability.snapshot.repository.PlotSnapshotRepository;
import org.example.sustainability.snapshot.repository.SeasonSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class SnapshotQueryService {

    FarmSnapshotRepository farmSnapshotRepository;
    PlotSnapshotRepository plotSnapshotRepository;
    SeasonSnapshotRepository seasonSnapshotRepository;
    CropSnapshotRepository cropSnapshotRepository;
    HarvestSnapshotRepository harvestSnapshotRepository;
    ExpenseSnapshotRepository expenseSnapshotRepository;

    public Optional<FarmContext> findFarm(Integer farmId) {
        FarmSnapshot snapshot = farmSnapshotRepository.findLatestByFarmId(farmId);
        return Optional.ofNullable(FarmContext.from(snapshot));
    }

    public Optional<PlotContext> findPlot(Integer plotId) {
        PlotSnapshot snapshot = plotSnapshotRepository.findLatestByPlotId(plotId);
        return Optional.ofNullable(PlotContext.from(snapshot));
    }

    public Optional<SeasonContext> findSeason(Integer seasonId) {
        SeasonSnapshot season = seasonSnapshotRepository.findLatestBySeasonId(seasonId);
        if (season == null) {
            return Optional.empty();
        }
        CropSnapshot crop = season.getCropId() != null
                ? cropSnapshotRepository.findLatestByCropId(season.getCropId())
                : null;
        return Optional.of(SeasonContext.from(season, crop));
    }

    public List<FarmContext> findFarmsForUser(Long userId) {
        return farmSnapshotRepository.findLatestFarmsForUser(userId).stream()
                .map(FarmContext::from)
                .toList();
    }

    public List<PlotContext> findPlotsByFarmId(Integer farmId) {
        return plotSnapshotRepository.findLatestPlotsByFarmId(farmId).stream()
                .map(PlotContext::from)
                .toList();
    }

    public List<PlotContext> findPlotsForUserAndFarm(Long userId, Integer farmId) {
        FarmSnapshot farm = farmSnapshotRepository.findLatestByFarmId(farmId);
        if (farm == null || farm.getUserId() == null || !farm.getUserId().equals(userId)) {
            return List.of();
        }
        return findPlotsByFarmId(farmId);
    }

    public List<PlotContext> findPlotsForUser(Long userId) {
        return farmSnapshotRepository.findLatestFarmsForUser(userId).stream()
                .flatMap(farm -> findPlotsByFarmId(farm.getFarmId()).stream())
                .toList();
    }

    public List<SeasonContext> findSeasonsByPlotIdOrderByStartDateDesc(Integer plotId) {
        return seasonSnapshotRepository.findLatestSeasonsByPlotIdOrderByStartDateDesc(plotId).stream()
                .map(season -> SeasonContext.from(
                        season,
                        season.getCropId() != null
                                ? cropSnapshotRepository.findLatestByCropId(season.getCropId())
                                : null))
                .toList();
    }

    public List<SeasonContext> findSeasonsByPlotIdOrderByStartDateAsc(Integer plotId) {
        return seasonSnapshotRepository.findLatestSeasonsByPlotIdOrderByStartDateAsc(plotId).stream()
                .map(season -> SeasonContext.from(
                        season,
                        season.getCropId() != null
                                ? cropSnapshotRepository.findLatestByCropId(season.getCropId())
                                : null))
                .toList();
    }

    public List<SeasonContext> findActiveSeasonsForUserOrderByStartDateDesc(Long userId) {
        return seasonSnapshotRepository.findActiveLatestSeasonsForUser(userId).stream()
                .map(season -> SeasonContext.from(
                        season,
                        season.getCropId() != null
                                ? cropSnapshotRepository.findLatestByCropId(season.getCropId())
                                : null))
                .toList();
    }

    public BigDecimal sumHarvestQuantityBySeasonId(Integer seasonId) {
        if (seasonId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = harvestSnapshotRepository.sumLatestQuantityBySeasonId(seasonId);
        return value != null ? value : BigDecimal.ZERO;
    }

    public BigDecimal sumExpenseTotalBySeasonId(Integer seasonId) {
        if (seasonId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = expenseSnapshotRepository.sumLatestTotalCostBySeasonId(seasonId);
        return value != null ? value : BigDecimal.ZERO;
    }

    public List<ExpenseFertilizerSnapshot> findFertilizerExpensesBySeasonId(Integer seasonId) {
        if (seasonId == null) {
            return List.of();
        }
        return expenseSnapshotRepository.findLatestFertilizerExpensesBySeasonId(seasonId).stream()
                .map(this::toFertilizerSnapshot)
                .toList();
    }

    public long countFieldLogsBySeasonAndLogType(Integer seasonId, String logType) {
        return 0L;
    }

    private ExpenseFertilizerSnapshot toFertilizerSnapshot(ExpenseSnapshot expense) {
        return new ExpenseFertilizerSnapshot(
                expense.getQuantity(),
                expense.getItemName(),
                expense.getNote()
        );
    }

    public record ExpenseFertilizerSnapshot(Integer quantity, String itemName, String note) {}

    public BigDecimal getMarketplaceRevenueBySeasonId(Integer seasonId) {
        if (seasonId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal val = seasonSnapshotRepository.getMarketplaceRevenueBySeasonId(seasonId);
        return val != null ? val : BigDecimal.ZERO;
    }

    public long countMarketplaceOrders() {
        return seasonSnapshotRepository.countMarketplaceOrders();
    }
}
