package org.example.QuanLyMuaVu.module.admin.service;

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
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.dto.request.AdminReportFilter;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminReportResponse;
import org.example.QuanLyMuaVu.module.admin.repository.AdminReportReadRepository;
import org.example.QuanLyMuaVu.module.admin.repository.AdminReportReadRepository.SeasonFinancialRow;
import org.example.QuanLyMuaVu.module.financial.port.ExpenseQueryPort;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminReportService {

    private static final String MARKETPLACE_REVENUE_STATUS_PENDING = "TODO_PENDING_MARKETPLACE_REVENUE_CONTRACT";

    private final ExpenseQueryPort expenseQueryPort;
    private final AdminReportReadRepository adminReportReadRepository;

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

        return rows.stream().map(row -> {
            BigDecimal quantity = normalize(row.getHarvestQuantityKg());
            BigDecimal revenue = normalize(row.getHarvestRevenue());
            return AdminReportResponse.RevenueReport.builder()
                    .seasonId(row.getSeasonId())
                    .seasonName(row.getSeasonName())
                    .cropName(row.getCropName())
                    .totalQuantity(quantity)
                    .totalRevenue(revenue)
                    .marketplaceRevenue(null)
                    .marketplaceRevenueStatus(MARKETPLACE_REVENUE_STATUS_PENDING)
                    .avgPricePerUnit(calculateAvgPrice(revenue, quantity))
                    .build();
        }).collect(Collectors.toList());
    }

    public List<AdminReportResponse.ProfitReport> getProfitReport(AdminReportFilter filter) {
        List<SeasonFinancialRow> rows = findSeasonRows(filter);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        return rows.stream().map(row -> {
            BigDecimal revenue = normalize(row.getHarvestRevenue());
            BigDecimal expense = normalize(row.getTotalExpense());
            BigDecimal profit = revenue.subtract(expense);
            return AdminReportResponse.ProfitReport.builder()
                    .seasonId(row.getSeasonId())
                    .seasonName(row.getSeasonName())
                    .cropName(row.getCropName())
                    .farmName(row.getFarmName())
                    .totalRevenue(revenue)
                    .marketplaceRevenue(null)
                    .marketplaceRevenueStatus(MARKETPLACE_REVENUE_STATUS_PENDING)
                    .totalExpense(expense)
                    .grossProfit(profit)
                    .profitMargin(calculatePercentage(profit, revenue))
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
        BigDecimal revenue = rows.stream().map(SeasonFinancialRow::getHarvestRevenue)
                .map(this::normalize).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossProfit = revenue.subtract(totalCost);
        BigDecimal marginPercent = calculatePercentage(grossProfit, revenue);
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
        warnings.add(localize(
                vietnameseLocale,
                "Marketplace revenue is excluded until marketplace report contract is finalized.",
                "Doanh thu marketplace chưa được tính cho đến khi hoàn tất contract báo cáo marketplace."));

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
                .revenue(revenue)
                .marketplaceRevenue(null)
                .marketplaceRevenueStatus(MARKETPLACE_REVENUE_STATUS_PENDING)
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
        List<org.example.QuanLyMuaVu.module.financial.entity.Expense> filteredExpenses = expenseQueryPort.findAllExpenseEntitiesBySeasonIds(seasonIds).stream()
                .filter(expense -> isWithinRange(expense.getExpenseDate(), from, to))
                .collect(Collectors.toList());

        boolean vietnameseLocale = isVietnameseLocale();
        Map<String, List<org.example.QuanLyMuaVu.module.financial.entity.Expense>> byCategory = filteredExpenses.stream()
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
        if (rows.isEmpty()) {
            return AdminReportResponse.RevenueAnalyticsResponse.builder()
                    .tableRows(Collections.emptyList())
                    .chartSeries(Collections.emptyList())
                    .totals(AdminReportResponse.RevenueTotals.builder()
                            .totalQuantity(BigDecimal.ZERO)
                            .totalRevenue(BigDecimal.ZERO)
                            .marketplaceRevenue(null)
                            .marketplaceRevenueStatus(MARKETPLACE_REVENUE_STATUS_PENDING)
                            .avgPrice(null)
                            .build())
                    .build();
        }

        List<AdminReportResponse.RevenueRow> tableRows = rows.stream().map(row -> {
            BigDecimal totalQuantity = normalize(row.getHarvestQuantityKg());
            BigDecimal totalRevenue = normalize(row.getHarvestRevenue());
            return AdminReportResponse.RevenueRow.builder()
                    .cropId(row.getCropId())
                    .cropName(row.getCropName())
                    .plotId(row.getPlotId())
                    .plotName(row.getPlotName())
                    .totalQuantity(totalQuantity)
                    .totalRevenue(totalRevenue)
                    .avgPrice(calculateAvgPrice(totalRevenue, totalQuantity))
                    .build();
        }).collect(Collectors.toList());

        BigDecimal totalQuantity = tableRows.stream().map(AdminReportResponse.RevenueRow::getTotalQuantity).map(this::normalize)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRevenue = tableRows.stream().map(AdminReportResponse.RevenueRow::getTotalRevenue).map(this::normalize)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminReportResponse.RevenueAnalyticsResponse.builder()
                .tableRows(tableRows)
                .chartSeries(tableRows)
                .totals(AdminReportResponse.RevenueTotals.builder()
                        .totalQuantity(totalQuantity)
                        .totalRevenue(totalRevenue)
                        .marketplaceRevenue(null)
                        .marketplaceRevenueStatus(MARKETPLACE_REVENUE_STATUS_PENDING)
                        .avgPrice(calculateAvgPrice(totalRevenue, totalQuantity))
                        .build())
                .build();
    }

    public AdminReportResponse.ProfitAnalyticsResponse getProfitAnalytics(AdminReportFilter filter) {
        List<SeasonFinancialRow> rows = findSeasonRows(filter);
        if (rows.isEmpty()) {
            return AdminReportResponse.ProfitAnalyticsResponse.builder()
                    .tableRows(Collections.emptyList())
                    .chartSeries(Collections.emptyList())
                    .totals(AdminReportResponse.ProfitTotals.builder()
                            .totalRevenue(BigDecimal.ZERO)
                            .marketplaceRevenue(null)
                            .marketplaceRevenueStatus(MARKETPLACE_REVENUE_STATUS_PENDING)
                            .totalCost(BigDecimal.ZERO)
                            .grossProfit(BigDecimal.ZERO)
                            .marginPercent(null)
                            .build())
                    .build();
        }

        List<AdminReportResponse.ProfitRow> tableRows = rows.stream().map(row -> {
            BigDecimal totalRevenue = normalize(row.getHarvestRevenue());
            BigDecimal totalCost = normalize(row.getTotalExpense());
            BigDecimal grossProfit = totalRevenue.subtract(totalCost);
            return AdminReportResponse.ProfitRow.builder()
                    .cropId(row.getCropId())
                    .cropName(row.getCropName())
                    .plotId(row.getPlotId())
                    .plotName(row.getPlotName())
                    .totalRevenue(totalRevenue)
                    .totalCost(totalCost)
                    .grossProfit(grossProfit)
                    .marginPercent(calculatePercentage(grossProfit, totalRevenue))
                    .build();
        }).collect(Collectors.toList());

        BigDecimal totalRevenue = tableRows.stream().map(AdminReportResponse.ProfitRow::getTotalRevenue).map(this::normalize)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = tableRows.stream().map(AdminReportResponse.ProfitRow::getTotalCost).map(this::normalize)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grossProfit = totalRevenue.subtract(totalCost);

        return AdminReportResponse.ProfitAnalyticsResponse.builder()
                .tableRows(tableRows)
                .chartSeries(tableRows)
                .totals(AdminReportResponse.ProfitTotals.builder()
                        .totalRevenue(totalRevenue)
                        .marketplaceRevenue(null)
                        .marketplaceRevenueStatus(MARKETPLACE_REVENUE_STATUS_PENDING)
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
            default -> throw new AppException(ErrorCode.BAD_REQUEST);
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

    private BigDecimal sumExpense(List<org.example.QuanLyMuaVu.module.financial.entity.Expense> expenses) {
        return expenses.stream().map(org.example.QuanLyMuaVu.module.financial.entity.Expense::getTotalCost).map(this::normalize).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<AdminReportResponse.CostTimeRow> buildCostTimeSeries(List<org.example.QuanLyMuaVu.module.financial.entity.Expense> expenses, String granularity) {
        String g = resolveGranularity(granularity);
        Map<LocalDate, BigDecimal> totals = new java.util.LinkedHashMap<>();
        for (org.example.QuanLyMuaVu.module.financial.entity.Expense expense : expenses) {
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
                    .append(',')
                    .append(csv(MARKETPLACE_REVENUE_STATUS_PENDING)).append(',')
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
                    .append(',')
                    .append(csv(MARKETPLACE_REVENUE_STATUS_PENDING)).append(',')
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
}
