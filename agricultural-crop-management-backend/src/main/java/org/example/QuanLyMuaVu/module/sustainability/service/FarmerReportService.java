package org.example.QuanLyMuaVu.module.sustainability.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminReportResponse;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.farm.service.FarmerOwnershipService;
import org.example.QuanLyMuaVu.module.financial.port.ExpenseQueryPort;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.port.HarvestQueryPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.sustainability.dto.request.FarmerReportFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class FarmerReportService {

    private static final String MARKETPLACE_REVENUE_STATUS_PENDING = "TODO_PENDING_MARKETPLACE_REVENUE_CONTRACT";

    SeasonQueryPort seasonQueryPort;
    HarvestQueryPort harvestQueryPort;
    ExpenseQueryPort expenseQueryPort;
    FarmAccessPort farmAccessService;
    FarmerOwnershipService ownershipService;

    public List<AdminReportResponse.YieldReport> getYieldReport(FarmerReportFilter filter) {
        return resolveSeasons(filter).stream()
                .map(season -> {
                    BigDecimal expectedYieldKg = normalize(season.getExpectedYieldKg());
                    BigDecimal actualYieldKg = normalize(harvestQueryPort.sumQuantityBySeasonId(season.getId()));

                    return AdminReportResponse.YieldReport.builder()
                            .seasonId(season.getId())
                            .seasonName(season.getSeasonName())
                            .cropName(season.getCrop() != null ? season.getCrop().getCropName() : null)
                            .plotName(season.getPlot() != null ? season.getPlot().getPlotName() : null)
                            .farmName(season.getPlot() != null && season.getPlot().getFarm() != null
                                    ? season.getPlot().getFarm().getName()
                                    : null)
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
                    BigDecimal totalExpense = normalize(expenseQueryPort.sumTotalCostBySeasonId(season.getId()));
                    BigDecimal totalYieldKg = normalize(harvestQueryPort.sumQuantityBySeasonId(season.getId()));

                    return AdminReportResponse.CostReport.builder()
                            .seasonId(season.getId())
                            .seasonName(season.getSeasonName())
                            .cropName(season.getCrop() != null ? season.getCrop().getCropName() : null)
                            .totalExpense(totalExpense)
                            .totalYieldKg(totalYieldKg)
                            .costPerKg(calculateCostPerKg(totalExpense, totalYieldKg))
                            .build();
                })
                .toList();
    }

    public List<AdminReportResponse.RevenueReport> getRevenueReport(FarmerReportFilter filter) {
        return resolveSeasons(filter).stream()
                .map(season -> {
                    BigDecimal totalQuantity = normalize(harvestQueryPort.sumQuantityBySeasonId(season.getId()));
                    BigDecimal totalRevenue = normalize(harvestQueryPort.sumRevenueBySeasonId(season.getId()));

                    return AdminReportResponse.RevenueReport.builder()
                            .seasonId(season.getId())
                            .seasonName(season.getSeasonName())
                            .cropName(season.getCrop() != null ? season.getCrop().getCropName() : null)
                            .totalQuantity(totalQuantity)
                            .totalRevenue(totalRevenue)
                            .marketplaceRevenue(null)
                            .marketplaceRevenueStatus(MARKETPLACE_REVENUE_STATUS_PENDING)
                            .avgPricePerUnit(calculateAveragePrice(totalRevenue, totalQuantity))
                            .build();
                })
                .toList();
    }

    public List<AdminReportResponse.ProfitReport> getProfitReport(FarmerReportFilter filter) {
        return resolveSeasons(filter).stream()
                .map(season -> {
                    BigDecimal totalRevenue = normalize(harvestQueryPort.sumRevenueBySeasonId(season.getId()));
                    BigDecimal totalExpense = normalize(expenseQueryPort.sumTotalCostBySeasonId(season.getId()));
                    BigDecimal grossProfit = totalRevenue.subtract(totalExpense);

                    return AdminReportResponse.ProfitReport.builder()
                            .seasonId(season.getId())
                            .seasonName(season.getSeasonName())
                            .cropName(season.getCrop() != null ? season.getCrop().getCropName() : null)
                            .farmName(season.getPlot() != null && season.getPlot().getFarm() != null
                                    ? season.getPlot().getFarm().getName()
                                    : null)
                            .totalRevenue(totalRevenue)
                            .marketplaceRevenue(null)
                            .marketplaceRevenueStatus(MARKETPLACE_REVENUE_STATUS_PENDING)
                            .totalExpense(totalExpense)
                            .grossProfit(grossProfit)
                            .profitMargin(calculatePercentage(grossProfit, totalRevenue))
                            .returnOnCost(calculatePercentage(grossProfit, totalExpense))
                            .build();
                })
                .toList();
    }

    private List<Season> resolveSeasons(FarmerReportFilter rawFilter) {
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

        List<Season> seasons;
        if (filter.getSeasonId() != null) {
            Season season = seasonQueryPort.findSeasonById(filter.getSeasonId())
                    .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
            farmAccessService.assertCurrentUserCanAccessSeason(season);
            seasons = List.of(season);
        } else {
            List<Integer> farmIds = farmAccessService.getAccessibleFarmIdsForCurrentUser();
            seasons = farmIds.isEmpty() ? List.of() : seasonQueryPort.findAllSeasonsByFarmIds(farmIds);
        }

        List<Season> filtered = seasons.stream()
                .filter(season -> filter.getFarmId() == null
                        || (season.getPlot() != null
                                && season.getPlot().getFarm() != null
                                && filter.getFarmId().equals(season.getPlot().getFarm().getId())))
                .filter(season -> filter.getPlotId() == null
                        || (season.getPlot() != null && filter.getPlotId().equals(season.getPlot().getId())))
                .filter(season -> filter.getCropId() == null
                        || (season.getCrop() != null && filter.getCropId().equals(season.getCrop().getId())))
                .filter(season -> isWithinDateRange(season, filter.getFromDate(), filter.getToDate()))
                .sorted(Comparator
                        .comparing(Season::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Season::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .toList();

        return applyPagination(filtered, filter);
    }

    private List<Season> applyPagination(List<Season> seasons, FarmerReportFilter filter) {
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

    private boolean isWithinDateRange(Season season, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return true;
        }
        LocalDate startDate = season.getStartDate();
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
