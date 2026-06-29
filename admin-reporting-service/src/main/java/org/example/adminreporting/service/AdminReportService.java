package org.example.adminreporting.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminreporting.dto.request.AdminReportFilter;
import org.example.adminreporting.dto.response.AdminReportResponse;
import org.example.adminreporting.entity.ExpenseSummary;
import org.example.adminreporting.repository.AdminReportReadRepository;
import org.example.adminreporting.repository.AdminReportReadRepository.SeasonFinancialRow;
import org.example.adminreporting.repository.ExpenseSummaryRepository;
import org.example.adminreporting.repository.MarketplaceOrderSummaryRepository;
import org.example.adminreporting.repository.TaskSummaryRepository;
import org.example.adminreporting.repository.IncidentSummaryRepository;
import org.example.adminreporting.repository.InventoryLotSummaryRepository;
import org.example.adminreporting.entity.IncidentSummary;
import org.example.adminreporting.entity.InventoryLotSummary;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminReportService {

    private final ExpenseSummaryRepository expenseSummaryRepository;
    private final AdminReportReadRepository adminReportReadRepository;
    private final MarketplaceOrderSummaryRepository marketplaceOrderSummaryRepository;
    private final TaskSummaryRepository taskSummaryRepository;
    private final IncidentSummaryRepository incidentSummaryRepository;
    private final InventoryLotSummaryRepository inventoryLotSummaryRepository;

    private String getMarketplaceRevenueStatus() {
        if (marketplaceOrderSummaryRepository.count() == 0) {
            return "MARKETPLACE_REVENUE_PROJECTION_EMPTY";
        }
        return "ACTIVE";
    }

    private BigDecimal getMarketplaceRevenue(BigDecimal dbValue) {
        if (marketplaceOrderSummaryRepository.count() == 0) {
            return BigDecimal.ZERO;
        }
        return dbValue != null ? dbValue : BigDecimal.ZERO;
    }

    public List<AdminReportResponse.YieldReport> getYieldReport(AdminReportFilter filter) {
        List<SeasonFinancialRow> rows = findSeasonRows(filter);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        return rows.stream().map(row -> {
            BigDecimal expected = normalize(row.getExpectedYieldKg());
            BigDecimal actual = normalize(row.getHarvestQuantityKg());
            return AdminReportResponse.YieldReport.builder()
                    .seasonId(row.getSeasonId())
                    .seasonName(row.getSeasonName())
                    .cropName(row.getCropName())
                    .plotName(row.getPlotName())
                    .farmName(row.getFarmName())
                    .expectedYieldKg(expected)
                    .actualYieldKg(actual)
                    .variancePercent(calculateVariancePercent(expected, actual))
                    .build();
        }).collect(Collectors.toList());
    }

    public List<AdminReportResponse.CostReport> getCostReport(AdminReportFilter filter) {
        List<SeasonFinancialRow> rows = findSeasonRows(filter);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        return rows.stream().map(row -> {
            BigDecimal expense = normalize(row.getTotalExpense());
            BigDecimal yield = normalize(row.getHarvestQuantityKg());
            return AdminReportResponse.CostReport.builder()
                    .seasonId(row.getSeasonId())
                    .seasonName(row.getSeasonName())
                    .cropName(row.getCropName())
                    .totalExpense(expense)
                    .totalYieldKg(yield)
                    .costPerKg(calculateCostPerKg(expense, yield))
                    .build();
        }).collect(Collectors.toList());
    }

    public List<AdminReportResponse.RevenueReport> getRevenueReport(AdminReportFilter filter) {
        List<SeasonFinancialRow> rows = findSeasonRows(filter);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        String status = getMarketplaceRevenueStatus();
        return rows.stream().map(row -> {
            BigDecimal quantity = normalize(row.getHarvestQuantityKg());
            BigDecimal harvestRev = normalize(row.getHarvestRevenue());
            BigDecimal mktRevenue = getMarketplaceRevenue(row.getMarketplaceRevenue());
            BigDecimal totalRev = harvestRev.add(mktRevenue);
            return AdminReportResponse.RevenueReport.builder()
                    .seasonId(row.getSeasonId())
                    .seasonName(row.getSeasonName())
                    .cropName(row.getCropName())
                    .totalQuantity(quantity)
                    .totalRevenue(totalRev)
                    .marketplaceRevenue(mktRevenue)
                    .marketplaceRevenueStatus(status)
                    .avgPricePerUnit(calculateAvgPrice(totalRev, quantity))
                    .build();
        }).collect(Collectors.toList());
    }

    public List<AdminReportResponse.ProfitReport> getProfitReport(AdminReportFilter filter) {
        List<SeasonFinancialRow> rows = findSeasonRows(filter);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        String status = getMarketplaceRevenueStatus();
        return rows.stream().map(row -> {
            BigDecimal harvestRev = normalize(row.getHarvestRevenue());
            BigDecimal mktRevenue = getMarketplaceRevenue(row.getMarketplaceRevenue());
            BigDecimal totalRev = harvestRev.add(mktRevenue);
            BigDecimal expense = normalize(row.getTotalExpense());
            BigDecimal profit = totalRev.subtract(expense);
            return AdminReportResponse.ProfitReport.builder()
                    .seasonId(row.getSeasonId())
                    .seasonName(row.getSeasonName())
                    .cropName(row.getCropName())
                    .farmName(row.getFarmName())
                    .totalRevenue(totalRev)
                    .marketplaceRevenue(mktRevenue)
                    .marketplaceRevenueStatus(status)
                    .totalExpense(expense)
                    .grossProfit(profit)
                    .profitMargin(calculatePercentage(profit, totalRev))
                    .returnOnCost(calculatePercentage(profit, expense))
                    .build();
        }).collect(Collectors.toList());
    }

    public AdminReportResponse.ReportSummary getSummary(AdminReportFilter filter) {
        List<SeasonFinancialRow> rows = findSeasonRows(filter);

        BigDecimal actualYield = rows.stream().map(SeasonFinancialRow::getHarvestQuantityKg)
                .map(this::normalize).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = rows.stream().map(SeasonFinancialRow::getTotalExpense)
                .map(this::normalize).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal harvestRevenue = rows.stream().map(SeasonFinancialRow::getHarvestRevenue)
                .map(this::normalize).reduce(BigDecimal.ZERO, BigDecimal::add);

        String status = getMarketplaceRevenueStatus();
        BigDecimal marketplaceRevenue = rows.stream().map(row -> getMarketplaceRevenue(row.getMarketplaceRevenue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = harvestRevenue.add(marketplaceRevenue);
        BigDecimal grossProfit = totalRevenue.subtract(totalCost);
        BigDecimal marginPercent = calculatePercentage(grossProfit, totalRevenue);
        BigDecimal costPerTon = calculateCostPerKg(totalCost, actualYield);

        boolean vietnameseLocale = isVietnameseLocale();
        List<String> warnings = new ArrayList<>();
        if (rows.isEmpty()) {
            warnings.add(localize(
                    vietnameseLocale,
                    "No seasons found for current filters",
                    "Không tìm thấy mùa vụ nào theo bộ lọc hiện tại"));
        }
        if (actualYield.compareTo(BigDecimal.ZERO) == 0 && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            warnings.add(localize(
                    vietnameseLocale,
                    "No harvested yield in selected range while expenses exist",
                    "Có chi phí trong khoảng đã chọn nhưng chưa có sản lượng thu hoạch"));
        }
        if ("MARKETPLACE_REVENUE_PROJECTION_EMPTY".equals(status)) {
            warnings.add("MARKETPLACE_REVENUE_PROJECTION_EMPTY");
        }

        AdminReportResponse.AppliedFilters appliedFilters = AdminReportResponse.AppliedFilters.builder()
                .dateFrom(filter.getEffectiveFromDate() != null ? filter.getEffectiveFromDate().toString() : null)
                .dateTo(filter.getEffectiveToDate() != null ? filter.getEffectiveToDate().toString() : null)
                .cropId(filter.getCropId())
                .farmId(filter.getFarmId())
                .plotId(filter.getPlotId())
                .varietyId(filter.getVarietyId())
                .areaMinHa(filter.getAreaMinHa())
                .areaMaxHa(filter.getAreaMaxHa())
                .build();

        return AdminReportResponse.ReportSummary.builder()
                .actualYield(actualYield)
                .totalCost(totalCost)
                .costPerTon(costPerTon)
                .revenue(totalRevenue)
                .marketplaceRevenue(marketplaceRevenue)
                .marketplaceRevenueStatus(status)
                .grossProfit(grossProfit)
                .marginPercent(marginPercent)
                .warnings(warnings)
                .appliedFilters(appliedFilters)
                .build();
    }

    public AdminReportResponse.YieldAnalyticsResponse getYieldAnalytics(AdminReportFilter filter) {
        List<SeasonFinancialRow> rows = findSeasonRows(filter);
        if (rows.isEmpty()) {
            return AdminReportResponse.YieldAnalyticsResponse.builder()
                    .tableRows(Collections.emptyList())
                    .chartSeries(Collections.emptyList())
                    .totals(AdminReportResponse.YieldAnalyticsTotals.builder()
                            .actualYield(BigDecimal.ZERO)
                            .harvestCount(0L)
                            .build())
                    .build();
        }

        List<AdminReportResponse.YieldAnalyticsRow> tableRows = rows.stream()
                .map(row -> AdminReportResponse.YieldAnalyticsRow.builder()
                        .farmId(row.getFarmId())
                        .farmName(row.getFarmName())
                        .plotId(row.getPlotId())
                        .plotName(row.getPlotName())
                        .cropId(row.getCropId())
                        .cropName(row.getCropName())
                        .varietyId(row.getVarietyId())
                        .varietyName(row.getVarietyName())
                        .actualYield(normalize(row.getHarvestQuantityKg()))
                        .harvestCount(row.getHarvestCount() != null ? row.getHarvestCount() : 0L)
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalYield = tableRows.stream().map(AdminReportResponse.YieldAnalyticsRow::getActualYield)
                .map(this::normalize).reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalHarvestCount = tableRows.stream().map(AdminReportResponse.YieldAnalyticsRow::getHarvestCount)
                .filter(Objects::nonNull).reduce(0L, Long::sum);

        return AdminReportResponse.YieldAnalyticsResponse.builder()
                .tableRows(tableRows)
                .chartSeries(tableRows)
                .totals(AdminReportResponse.YieldAnalyticsTotals.builder()
                        .actualYield(totalYield)
                        .harvestCount(totalHarvestCount)
                        .build())
                .build();
    }

    public AdminReportResponse.CostAnalyticsResponse getCostAnalytics(AdminReportFilter filter, String granularity) {
        List<SeasonFinancialRow> rows = findSeasonRows(filter);
        if (rows.isEmpty()) {
            return AdminReportResponse.CostAnalyticsResponse.builder()
                    .tableRows(Collections.emptyList())
                    .chartSeries(Collections.emptyList())
                    .totals(AdminReportResponse.CostTotals.builder().totalCost(BigDecimal.ZERO).expenseCount(0L).build())
                    .vendorRows(Collections.emptyList())
                    .timeSeries(Collections.emptyList())
                    .build();
        }

        Set<Integer> seasonIds = collectSeasonIds(rows);
        LocalDate from = filter.getEffectiveFromDate();
        LocalDate to = filter.getEffectiveToDate();
        List<ExpenseSummary> filteredExpenses = expenseSummaryRepository.findBySeasonIdIn(seasonIds).stream()
                .filter(expense -> isWithinRange(expense.getExpenseDate(), from, to))
                .collect(Collectors.toList());

        boolean vietnameseLocale = isVietnameseLocale();
        Map<String, List<ExpenseSummary>> byCategory = filteredExpenses.stream()
                .collect(Collectors.groupingBy(expense -> normalizeCategory(expense.getCategory(), vietnameseLocale)));

        List<AdminReportResponse.CostCategoryRow> tableRows = byCategory.entrySet().stream()
                .map(entry -> AdminReportResponse.CostCategoryRow.builder()
                        .category(entry.getKey())
                        .totalCost(sumExpense(entry.getValue()))
                        .expenseCount((long) entry.getValue().size())
                        .build())
                .sorted(Comparator.comparing(AdminReportResponse.CostCategoryRow::getTotalCost,
                        Comparator.nullsFirst(BigDecimal::compareTo)).reversed())
                .collect(Collectors.toList());

        BigDecimal totalCost = tableRows.stream().map(AdminReportResponse.CostCategoryRow::getTotalCost).map(this::normalize)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long expenseCount = tableRows.stream().map(AdminReportResponse.CostCategoryRow::getExpenseCount)
                .filter(Objects::nonNull).reduce(0L, Long::sum);

        List<AdminReportResponse.CostVendorRow> vendorRows = filteredExpenses.stream()
                .collect(Collectors.groupingBy(expense -> {
                    String value = expense.getItemName();
                    return (value == null || value.trim().isEmpty()) ? unassignedLabel(vietnameseLocale) : value.trim();
                }))
                .entrySet().stream()
                .map(entry -> AdminReportResponse.CostVendorRow.builder()
                        .vendorId(null)
                        .vendorName(entry.getKey())
                        .totalCost(sumExpense(entry.getValue()))
                        .expenseCount((long) entry.getValue().size())
                        .build())
                .sorted(Comparator.comparing(AdminReportResponse.CostVendorRow::getTotalCost,
                        Comparator.nullsFirst(BigDecimal::compareTo)).reversed())
                .collect(Collectors.toList());

        return AdminReportResponse.CostAnalyticsResponse.builder()
                .tableRows(tableRows)
                .chartSeries(tableRows)
                .totals(AdminReportResponse.CostTotals.builder().totalCost(totalCost).expenseCount(expenseCount).build())
                .vendorRows(vendorRows)
                .timeSeries(buildCostTimeSeries(filteredExpenses, granularity))
                .build();
    }

    public AdminReportResponse.RevenueAnalyticsResponse getRevenueAnalytics(AdminReportFilter filter) {
        List<SeasonFinancialRow> rows = findSeasonRows(filter);
        String status = getMarketplaceRevenueStatus();
        if (rows.isEmpty()) {
            return AdminReportResponse.RevenueAnalyticsResponse.builder()
                    .tableRows(Collections.emptyList())
                    .chartSeries(Collections.emptyList())
                    .totals(AdminReportResponse.RevenueTotals.builder()
                            .totalQuantity(BigDecimal.ZERO)
                            .totalRevenue(BigDecimal.ZERO)
                            .marketplaceRevenue(getMarketplaceRevenue(BigDecimal.ZERO))
                            .marketplaceRevenueStatus(status)
                            .avgPrice(null)
                            .build())
                    .build();
        }

        List<AdminReportResponse.RevenueRow> tableRows = rows.stream().map(row -> {
            BigDecimal totalQuantity = normalize(row.getHarvestQuantityKg());
            BigDecimal harvestRev = normalize(row.getHarvestRevenue());
            BigDecimal mktRev = getMarketplaceRevenue(row.getMarketplaceRevenue());
            BigDecimal totalRevenue = harvestRev.add(mktRev);
            return AdminReportResponse.RevenueRow.builder()
                    .cropId(row.getCropId())
                    .cropName(row.getCropName())
                    .plotId(row.getPlotId())
                    .plotName(row.getPlotName())
                    .totalQuantity(totalQuantity)
                    .totalRevenue(totalRevenue)
                    .marketplaceRevenue(mktRev)
                    .marketplaceRevenueStatus(status)
                    .avgPrice(calculateAvgPrice(totalRevenue, totalQuantity))
                    .build();
        }).collect(Collectors.toList());

        BigDecimal totalQuantity = tableRows.stream().map(AdminReportResponse.RevenueRow::getTotalQuantity).map(this::normalize)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal harvestRevenue = rows.stream().map(SeasonFinancialRow::getHarvestRevenue).map(this::normalize)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal marketplaceRevenue = rows.stream().map(row -> getMarketplaceRevenue(row.getMarketplaceRevenue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRevenue = harvestRevenue.add(marketplaceRevenue);

        return AdminReportResponse.RevenueAnalyticsResponse.builder()
                .tableRows(tableRows)
                .chartSeries(tableRows)
                .totals(AdminReportResponse.RevenueTotals.builder()
                        .totalQuantity(totalQuantity)
                        .totalRevenue(totalRevenue)
                        .marketplaceRevenue(marketplaceRevenue)
                        .marketplaceRevenueStatus(status)
                        .avgPrice(calculateAvgPrice(totalRevenue, totalQuantity))
                        .build())
                .build();
    }

    public AdminReportResponse.ProfitAnalyticsResponse getProfitAnalytics(AdminReportFilter filter) {
        List<SeasonFinancialRow> rows = findSeasonRows(filter);
        String status = getMarketplaceRevenueStatus();
        if (rows.isEmpty()) {
            return AdminReportResponse.ProfitAnalyticsResponse.builder()
                    .tableRows(Collections.emptyList())
                    .chartSeries(Collections.emptyList())
                    .totals(AdminReportResponse.ProfitTotals.builder()
                            .totalRevenue(BigDecimal.ZERO)
                            .marketplaceRevenue(getMarketplaceRevenue(BigDecimal.ZERO))
                            .marketplaceRevenueStatus(status)
                            .totalCost(BigDecimal.ZERO)
                            .grossProfit(BigDecimal.ZERO)
                            .marginPercent(null)
                            .build())
                    .build();
        }

        List<AdminReportResponse.ProfitRow> tableRows = rows.stream().map(row -> {
            BigDecimal harvestRev = normalize(row.getHarvestRevenue());
            BigDecimal mktRev = getMarketplaceRevenue(row.getMarketplaceRevenue());
            BigDecimal totalRevenue = harvestRev.add(mktRev);
            BigDecimal totalCost = normalize(row.getTotalExpense());
            BigDecimal grossProfit = totalRevenue.subtract(totalCost);
            return AdminReportResponse.ProfitRow.builder()
                    .cropId(row.getCropId())
                    .cropName(row.getCropName())
                    .plotId(row.getPlotId())
                    .plotName(row.getPlotName())
                    .totalRevenue(totalRevenue)
                    .marketplaceRevenue(mktRev)
                    .marketplaceRevenueStatus(status)
                    .totalCost(totalCost)
                    .grossProfit(grossProfit)
                    .marginPercent(calculatePercentage(grossProfit, totalRevenue))
                    .build();
        }).collect(Collectors.toList());

        BigDecimal harvestRevenue = rows.stream().map(SeasonFinancialRow::getHarvestRevenue).map(this::normalize)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal marketplaceRevenue = rows.stream().map(row -> getMarketplaceRevenue(row.getMarketplaceRevenue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRevenue = harvestRevenue.add(marketplaceRevenue);
        BigDecimal totalCost = tableRows.stream().map(AdminReportResponse.ProfitRow::getTotalCost).map(this::normalize)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grossProfit = totalRevenue.subtract(totalCost);

        return AdminReportResponse.ProfitAnalyticsResponse.builder()
                .tableRows(tableRows)
                .chartSeries(tableRows)
                .totals(AdminReportResponse.ProfitTotals.builder()
                        .totalRevenue(totalRevenue)
                        .marketplaceRevenue(marketplaceRevenue)
                        .marketplaceRevenueStatus(status)
                        .totalCost(totalCost)
                        .grossProfit(grossProfit)
                        .marginPercent(calculatePercentage(grossProfit, totalRevenue))
                        .build())
                .build();
    }

    public String exportReportCsv(String tab, AdminReportFilter filter, String granularity) {
        String resolvedTab = tab != null ? tab.trim().toLowerCase(Locale.ROOT) : "";
        return switch (resolvedTab) {
            case "yield" -> buildYieldCsv(getYieldAnalytics(filter));
            case "cost" -> buildCostCsv(getCostAnalytics(filter, granularity));
            case "revenue" -> buildRevenueCsv(getRevenueAnalytics(filter));
            case "profit" -> buildProfitCsv(getProfitAnalytics(filter));
            default -> throw new IllegalArgumentException("Invalid report tab: " + tab);
        };
    }

    private List<SeasonFinancialRow> findSeasonRows(AdminReportFilter filter) {
        return adminReportReadRepository.findSeasonFinancialRows(filter);
    }

    private Set<Integer> collectSeasonIds(List<SeasonFinancialRow> rows) {
        return rows.stream()
                .map(SeasonFinancialRow::getSeasonId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isWithinRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) {
            return false;
        }
        if (from != null && date.isBefore(from)) {
            return false;
        }
        if (to != null && !date.isBefore(to)) {
            return false;
        }
        return true;
    }

    private String normalizeCategory(String category, boolean vietnameseLocale) {
        return (category == null || category.trim().isEmpty()) ? uncategorizedLabel(vietnameseLocale) : category.trim();
    }

    private BigDecimal sumExpense(List<ExpenseSummary> expenses) {
        return expenses.stream().map(ExpenseSummary::getTotalCost).map(this::normalize).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<AdminReportResponse.CostTimeRow> buildCostTimeSeries(List<ExpenseSummary> expenses, String granularity) {
        String g = resolveGranularity(granularity);
        Map<LocalDate, BigDecimal> totals = new java.util.LinkedHashMap<>();
        for (ExpenseSummary expense : expenses) {
            LocalDate date = expense.getExpenseDate();
            if (date == null) {
                continue;
            }
            LocalDate periodStart = "DAY".equals(g)
                    ? date
                    : ("WEEK".equals(g) ? date.with(DayOfWeek.MONDAY) : date.withDayOfMonth(1));
            totals.put(periodStart, totals.getOrDefault(periodStart, BigDecimal.ZERO).add(normalize(expense.getTotalCost())));
        }
        return totals.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> AdminReportResponse.CostTimeRow.builder()
                        .periodStart(entry.getKey().toString())
                        .label(buildTimeLabel(entry.getKey(), g))
                        .totalCost(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private String resolveGranularity(String granularity) {
        if (granularity == null) {
            return "MONTH";
        }
        String normalized = granularity.trim().toUpperCase(Locale.ROOT);
        return ("DAY".equals(normalized) || "WEEK".equals(normalized) || "MONTH".equals(normalized))
                ? normalized
                : "MONTH";
    }

    private String buildTimeLabel(LocalDate periodStart, String granularity) {
        if ("DAY".equals(granularity)) {
            return periodStart.toString();
        }
        if ("WEEK".equals(granularity)) {
            int week = periodStart.get(WeekFields.ISO.weekOfWeekBasedYear());
            int year = periodStart.get(WeekFields.ISO.weekBasedYear());
            return year + "-W" + String.format("%02d", week);
        }
        return periodStart.getYear() + "-" + String.format("%02d", periodStart.getMonthValue());
    }

    private String uncategorizedLabel(boolean vietnameseLocale) {
        return localize(vietnameseLocale, "Uncategorized", "Chưa phân loại");
    }

    private String unassignedLabel(boolean vietnameseLocale) {
        return localize(vietnameseLocale, "Unassigned", "Chưa gán");
    }

    private String yieldCsvHeader() {
        return localize(
                "farm_name,plot_name,crop_name,variety_name,actual_yield_kg,harvest_count\n",
                "Tên nông trại,Tên thửa,Tên cây trồng,Tên giống,Sản lượng thực tế (kg),Số lần thu hoạch\n");
    }

    private String costCsvHeader() {
        return localize(
                "section,key,total_cost,expense_count,period_start\n",
                "Phân nhóm,Khóa,Tổng chi phí,Số chi phí,Kỳ bắt đầu\n");
    }

    private String revenueCsvHeader() {
        return localize(
                "crop_name,plot_name,total_quantity_kg,total_revenue,marketplace_revenue,marketplace_revenue_status,avg_price\n",
                "Tên cây trồng,Tên thửa,Tổng sản lượng (kg),Tổng doanh thu,Doanh thu marketplace,Trạng thái doanh thu marketplace,Giá trung bình\n");
    }

    private String profitCsvHeader() {
        return localize(
                "crop_name,plot_name,total_revenue,marketplace_revenue,marketplace_revenue_status,total_cost,gross_profit,margin_percent\n",
                "Tên cây trồng,Tên thửa,Tổng doanh thu,Doanh thu marketplace,Trạng thái doanh thu marketplace,Tổng chi phí,Lợi nhuận gộp,Tỷ suất lợi nhuận\n");
    }

    private String costCategorySection() {
        return localize("category", "Danh mục");
    }

    private String costVendorSection() {
        return localize("vendor", "Nhà cung cấp");
    }

    private String costTimeSection() {
        return localize("time", "Thời gian");
    }

    private String buildYieldCsv(AdminReportResponse.YieldAnalyticsResponse response) {
        StringBuilder b = new StringBuilder(yieldCsvHeader());
        for (AdminReportResponse.YieldAnalyticsRow row : response.getTableRows()) {
            b.append(csv(row.getFarmName())).append(',')
                    .append(csv(row.getPlotName())).append(',')
                    .append(csv(row.getCropName())).append(',')
                    .append(csv(row.getVarietyName())).append(',')
                    .append(csvNumber(row.getActualYield())).append(',')
                    .append(csvLong(row.getHarvestCount())).append('\n');
        }
        return b.toString();
    }

    private String buildCostCsv(AdminReportResponse.CostAnalyticsResponse response) {
        StringBuilder b = new StringBuilder(costCsvHeader());
        for (AdminReportResponse.CostCategoryRow row : response.getTableRows()) {
            b.append(costCategorySection()).append(',').append(csv(row.getCategory())).append(',')
                    .append(csvNumber(row.getTotalCost())).append(',')
                    .append(csvLong(row.getExpenseCount())).append(',')
                    .append('\n');
        }
        for (AdminReportResponse.CostVendorRow row : response.getVendorRows()) {
            b.append(costVendorSection()).append(',').append(csv(row.getVendorName())).append(',')
                    .append(csvNumber(row.getTotalCost())).append(',')
                    .append(csvLong(row.getExpenseCount())).append(',')
                    .append('\n');
        }
        for (AdminReportResponse.CostTimeRow row : response.getTimeSeries()) {
            b.append(costTimeSection()).append(',').append(csv(row.getLabel())).append(',')
                    .append(csvNumber(row.getTotalCost())).append(',')
                    .append(',')
                    .append(csv(row.getPeriodStart())).append('\n');
        }
        return b.toString();
    }

    private String buildRevenueCsv(AdminReportResponse.RevenueAnalyticsResponse response) {
        StringBuilder b = new StringBuilder(revenueCsvHeader());
        for (AdminReportResponse.RevenueRow row : response.getTableRows()) {
            b.append(csv(row.getCropName())).append(',')
                    .append(csv(row.getPlotName())).append(',')
                    .append(csvNumber(row.getTotalQuantity())).append(',')
                    .append(csvNumber(row.getTotalRevenue())).append(',')
                    .append(csvNumber(row.getMarketplaceRevenue())).append(',')
                    .append(csv(row.getMarketplaceRevenueStatus())).append(',')
                    .append(csvNumber(row.getAvgPrice())).append('\n');
        }
        return b.toString();
    }

    private String buildProfitCsv(AdminReportResponse.ProfitAnalyticsResponse response) {
        StringBuilder b = new StringBuilder(profitCsvHeader());
        for (AdminReportResponse.ProfitRow row : response.getTableRows()) {
            b.append(csv(row.getCropName())).append(',')
                    .append(csv(row.getPlotName())).append(',')
                    .append(csvNumber(row.getTotalRevenue())).append(',')
                    .append(csvNumber(row.getMarketplaceRevenue())).append(',')
                    .append(csv(row.getMarketplaceRevenueStatus())).append(',')
                    .append(csvNumber(row.getTotalCost())).append(',')
                    .append(csvNumber(row.getGrossProfit())).append(',')
                    .append(csvNumber(row.getMarginPercent())).append('\n');
        }
        return b.toString();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String csvNumber(BigDecimal value) {
        return value != null ? value.toPlainString() : "";
    }

    private String csvLong(Long value) {
        return value != null ? String.valueOf(value) : "";
    }

    private BigDecimal normalize(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal calculateVariancePercent(BigDecimal expected, BigDecimal actual) {
        if (expected == null || expected.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return actual.subtract(expected).multiply(BigDecimal.valueOf(100)).divide(expected, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCostPerKg(BigDecimal expense, BigDecimal yield) {
        if (yield == null || yield.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return expense.divide(yield, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAvgPrice(BigDecimal revenue, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return revenue.divide(quantity, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePercentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private boolean isVietnameseLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        return locale != null && "vi".equalsIgnoreCase(locale.getLanguage());
    }

    private String localize(String english, String vietnamese) {
        return localize(isVietnameseLocale(), english, vietnamese);
    }

    private String localize(boolean vietnameseLocale, String english, String vietnamese) {
        return vietnameseLocale ? vietnamese : english;
    }

    public AdminReportResponse.TaskPerformanceReport getTaskPerformance(Integer year) {
        long totalTasks = taskSummaryRepository.countTasksByYear(year);
        long completedTasks = taskSummaryRepository.countTasksByYearAndStatus(year, "DONE");
        long overdueTasks = taskSummaryRepository.countTasksByYearAndStatus(year, "OVERDUE");
        long pendingTasks = taskSummaryRepository.countTasksByYearAndStatus(year, "PENDING");
        long inProgressTasks = taskSummaryRepository.countTasksByYearAndStatus(year, "IN_PROGRESS");
        long cancelledTasks = taskSummaryRepository.countTasksByYearAndStatus(year, "CANCELLED");

        BigDecimal completionRate = totalTasks > 0
                ? BigDecimal.valueOf(completedTasks).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalTasks), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal overdueRate = totalTasks > 0
                ? BigDecimal.valueOf(overdueTasks).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalTasks), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return AdminReportResponse.TaskPerformanceReport.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .overdueTasks(overdueTasks)
                .pendingTasks(pendingTasks)
                .inProgressTasks(inProgressTasks)
                .cancelledTasks(cancelledTasks)
                .completionRate(completionRate)
                .overdueRate(overdueRate)
                .build();
    }

    public List<AdminReportResponse.InventoryOnHandReport> getInventoryOnHandReport() {
        List<InventoryLotSummary> lots = inventoryLotSummaryRepository.findAll();
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(30);

        Map<Integer, List<InventoryLotSummary>> grouped = lots.stream()
                .filter(lot -> lot.getWarehouseId() != null)
                .collect(Collectors.groupingBy(InventoryLotSummary::getWarehouseId));

        return grouped.entrySet().stream().map(entry -> {
            Integer warehouseId = entry.getKey();
            List<InventoryLotSummary> groupLots = entry.getValue();
            String warehouseName = groupLots.get(0).getWarehouseName();
            String farmName = groupLots.get(0).getFarmName();

            int totalLots = groupLots.size();
            BigDecimal totalQuantity = groupLots.stream()
                    .map(InventoryLotSummary::getQuantityOnHand)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int expired = (int) groupLots.stream()
                    .filter(lot -> lot.getExpiryDate() != null && lot.getExpiryDate().isBefore(today))
                    .count();

            int expiringSoon = (int) groupLots.stream()
                    .filter(lot -> lot.getExpiryDate() != null && !lot.getExpiryDate().isBefore(today) && !lot.getExpiryDate().isAfter(cutoff))
                    .count();

            return AdminReportResponse.InventoryOnHandReport.builder()
                    .warehouseId(warehouseId)
                    .warehouseName(warehouseName)
                    .farmName(farmName)
                    .totalLots(totalLots)
                    .totalQuantityOnHand(totalQuantity)
                    .expiredLots(expired)
                    .expiringSoonLots(expiringSoon)
                    .build();
        }).collect(Collectors.toList());
    }

    public AdminReportResponse.IncidentStatisticsReport getIncidentStatistics(Integer year) {
        List<IncidentSummary> incidents = incidentSummaryRepository.findIncidentsByYear(year);

        Map<String, Long> byIncidentType = incidents.stream()
                .filter(i -> i.getIncidentType() != null)
                .collect(Collectors.groupingBy(IncidentSummary::getIncidentType, Collectors.counting()));

        Map<String, Long> bySeverity = incidents.stream()
                .filter(i -> i.getSeverity() != null)
                .collect(Collectors.groupingBy(IncidentSummary::getSeverity, Collectors.counting()));

        Map<String, Long> byStatus = incidents.stream()
                .filter(i -> i.getStatus() != null)
                .collect(Collectors.groupingBy(IncidentSummary::getStatus, Collectors.counting()));

        long totalCount = incidents.size();
        long openCount = incidents.stream()
                .filter(i -> "OPEN".equalsIgnoreCase(i.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(i.getStatus()))
                .count();
        long resolvedCount = incidents.stream()
                .filter(i -> "RESOLVED".equalsIgnoreCase(i.getStatus()))
                .count();

        List<Long> resolutionTimes = incidents.stream()
                .filter(i -> "RESOLVED".equalsIgnoreCase(i.getStatus()) && i.getResolvedAt() != null && i.getCreatedAt() != null)
                .map(i -> java.time.Duration.between(i.getCreatedAt(), i.getResolvedAt()).toDays())
                .collect(Collectors.toList());

        BigDecimal averageResolutionDays = BigDecimal.ZERO;
        if (!resolutionTimes.isEmpty()) {
            double avg = resolutionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            averageResolutionDays = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
        }

        return AdminReportResponse.IncidentStatisticsReport.builder()
                .byIncidentType(byIncidentType)
                .bySeverity(bySeverity)
                .byStatus(byStatus)
                .totalCount(totalCount)
                .openCount(openCount)
                .resolvedCount(resolvedCount)
                .averageResolutionDays(averageResolutionDays)
                .build();
    }
}
