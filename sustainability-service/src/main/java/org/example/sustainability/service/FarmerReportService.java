package org.example.sustainability.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.sustainability.config.CurrentUserService;
import org.example.sustainability.dto.request.FarmerReportFilter;
import org.example.sustainability.dto.response.AdminReportResponse;
import org.example.sustainability.exception.AppException;
import org.example.sustainability.exception.ErrorCode;
import org.example.sustainability.snapshot.model.FarmContext;
import org.example.sustainability.snapshot.model.PlotContext;
import org.example.sustainability.snapshot.model.SeasonContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class FarmerReportService {

    SnapshotQueryService snapshotQueryService;
    FarmerOwnershipService ownershipService;
    CurrentUserService currentUserService;

    public List<AdminReportResponse.YieldReport> getYieldReport(FarmerReportFilter filter) {
        return resolveSeasons(filter).stream()
                .map(season -> {
                    PlotContext plot = season.plotId() != null
                            ? snapshotQueryService.findPlot(season.plotId()).orElse(null)
                            : null;
                    FarmContext farm = plot != null && plot.farmId() != null
                            ? snapshotQueryService.findFarm(plot.farmId()).orElse(null)
                            : null;
                    BigDecimal expectedYieldKg = normalize(season.expectedYieldKg());
                    BigDecimal actualYieldKg = normalize(snapshotQueryService.sumHarvestQuantityBySeasonId(season.id()));

                    return AdminReportResponse.YieldReport.builder()
                            .seasonId(season.id())
                            .seasonName(season.seasonName())
                            .cropName(season.cropName())
                            .plotName(plot != null ? plot.plotName() : null)
                            .farmName(farm != null ? farm.name() : null)
                            .expectedYieldKg(expectedYieldKg)
                            .actualYieldKg(actualYieldKg)
                            .variancePercent(calculateVariancePercent(expectedYieldKg, actualYieldKg))
                            .build();
                })
                .toList();
    }

    public List<AdminReportResponse.CostReport> getCostReport(FarmerReportFilter filter) {
        return resolveSeasons(filter).stream()
                .map(season -> {
                    BigDecimal totalExpense = normalize(snapshotQueryService.sumExpenseTotalBySeasonId(season.id()));
                    BigDecimal totalYieldKg = normalize(snapshotQueryService.sumHarvestQuantityBySeasonId(season.id()));

                    return AdminReportResponse.CostReport.builder()
                            .seasonId(season.id())
                            .seasonName(season.seasonName())
                            .cropName(season.cropName())
                            .totalExpense(totalExpense)
                            .totalYieldKg(totalYieldKg)
                            .costPerKg(calculateCostPerKg(totalExpense, totalYieldKg))
                            .build();
                })
                .toList();
    }

    private String getMarketplaceRevenueStatus() {
        if (snapshotQueryService.countMarketplaceOrders() == 0) {
            return "MARKETPLACE_REVENUE_PROJECTION_EMPTY";
        }
        return "ACTIVE";
    }

    private BigDecimal getMarketplaceRevenue(Integer seasonId) {
        if (snapshotQueryService.countMarketplaceOrders() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rev = snapshotQueryService.getMarketplaceRevenueBySeasonId(seasonId);
        return rev != null ? rev : BigDecimal.ZERO;
    }

    public List<AdminReportResponse.RevenueReport> getRevenueReport(FarmerReportFilter filter) {
        String status = getMarketplaceRevenueStatus();
        return resolveSeasons(filter).stream()
                .map(season -> {
                    BigDecimal totalQuantity = normalize(snapshotQueryService.sumHarvestQuantityBySeasonId(season.id()));
                    BigDecimal mktRevenue = getMarketplaceRevenue(season.id());
                    BigDecimal totalRevenue = mktRevenue; // Only marketplace revenue for now (no local harvest revenue column in snapshots)

                    return AdminReportResponse.RevenueReport.builder()
                            .seasonId(season.id())
                            .seasonName(season.seasonName())
                            .cropName(season.cropName())
                            .totalQuantity(totalQuantity)
                            .totalRevenue(totalRevenue)
                            .marketplaceRevenue(mktRevenue)
                            .marketplaceRevenueStatus(status)
                            .avgPricePerUnit(calculateAveragePrice(totalRevenue, totalQuantity))
                            .build();
                })
                .toList();
    }

    public List<AdminReportResponse.ProfitReport> getProfitReport(FarmerReportFilter filter) {
        String status = getMarketplaceRevenueStatus();
        return resolveSeasons(filter).stream()
                .map(season -> {
                    PlotContext plot = season.plotId() != null
                            ? snapshotQueryService.findPlot(season.plotId()).orElse(null)
                            : null;
                    FarmContext farm = plot != null && plot.farmId() != null
                            ? snapshotQueryService.findFarm(plot.farmId()).orElse(null)
                            : null;
                    BigDecimal mktRevenue = getMarketplaceRevenue(season.id());
                    BigDecimal totalRevenue = mktRevenue;
                    BigDecimal totalExpense = normalize(snapshotQueryService.sumExpenseTotalBySeasonId(season.id()));
                    BigDecimal grossProfit = totalRevenue.subtract(totalExpense);

                    return AdminReportResponse.ProfitReport.builder()
                            .seasonId(season.id())
                            .seasonName(season.seasonName())
                            .cropName(season.cropName())
                            .farmName(farm != null ? farm.name() : null)
                            .totalRevenue(totalRevenue)
                            .marketplaceRevenue(mktRevenue)
                            .marketplaceRevenueStatus(status)
                            .totalExpense(totalExpense)
                            .grossProfit(grossProfit)
                            .profitMargin(calculatePercentage(grossProfit, totalRevenue))
                            .returnOnCost(calculatePercentage(grossProfit, totalExpense))
                            .build();
                })
                .toList();
    }

    private List<SeasonContext> resolveSeasons(FarmerReportFilter rawFilter) {
        FarmerReportFilter filter = rawFilter != null ? rawFilter : FarmerReportFilter.builder().build();

        if (filter.getSeasonId() != null && filter.getSeasonId() <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        if (filter.getFarmId() != null) {
            ownershipService.requireOwnedFarm(filter.getFarmId());
        }
        if (filter.getPlotId() != null) {
            ownershipService.requireOwnedPlot(filter.getPlotId());
        }

        List<SeasonContext> seasons;
        if (filter.getSeasonId() != null) {
            seasons = List.of(ownershipService.requireOwnedSeason(filter.getSeasonId()));
        } else {
            Long userId = currentUserService.getCurrentUserId();
            List<SeasonContext> all = new ArrayList<>();
            for (FarmContext farm : snapshotQueryService.findFarmsForUser(userId)) {
                for (PlotContext plot : snapshotQueryService.findPlotsByFarmId(farm.id())) {
                    all.addAll(snapshotQueryService.findSeasonsByPlotIdOrderByStartDateDesc(plot.id()));
                }
            }
            seasons = all;
        }

        List<SeasonContext> filtered = seasons.stream()
                .filter(season -> filter.getFarmId() == null || (season.farmId() != null && filter.getFarmId().equals(season.farmId())))
                .filter(season -> filter.getPlotId() == null || (season.plotId() != null && filter.getPlotId().equals(season.plotId())))
                .filter(season -> filter.getCropId() == null || (season.cropId() != null && filter.getCropId().equals(season.cropId())))
                .filter(season -> isWithinDateRange(season, filter.getFromDate(), filter.getToDate()))
                .sorted(Comparator
                        .comparing(SeasonContext::startDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SeasonContext::id, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .toList();

        return applyPagination(filtered, filter);
    }

    private List<SeasonContext> applyPagination(List<SeasonContext> seasons, FarmerReportFilter filter) {
        if (!filter.hasPagination()) {
            return seasons;
        }

        int size = filter.getSafeSize();
        int page = filter.getSafePage();
        int fromIndex = page * size;
        if (fromIndex >= seasons.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + size, seasons.size());
        return seasons.subList(fromIndex, toIndex);
    }

    private boolean isWithinDateRange(SeasonContext season, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return true;
        }
        LocalDate startDate = season.startDate();
        if (startDate == null) {
            return false;
        }
        if (fromDate != null && startDate.isBefore(fromDate)) {
            return false;
        }
        if (toDate != null && !startDate.isBefore(toDate)) {
            return false;
        }
        return true;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal calculateVariancePercent(BigDecimal expected, BigDecimal actual) {
        if (expected == null || expected.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return actual.subtract(expected)
                .multiply(BigDecimal.valueOf(100))
                .divide(expected, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCostPerKg(BigDecimal totalExpense, BigDecimal totalYieldKg) {
        if (totalYieldKg == null || totalYieldKg.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return totalExpense.divide(totalYieldKg, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAveragePrice(BigDecimal totalRevenue, BigDecimal totalQuantity) {
        if (totalQuantity == null || totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return totalRevenue.divide(totalQuantity, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePercentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
