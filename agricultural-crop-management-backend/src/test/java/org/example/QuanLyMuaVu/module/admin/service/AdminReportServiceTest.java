package org.example.QuanLyMuaVu.module.admin.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.example.QuanLyMuaVu.module.admin.dto.request.AdminReportFilter;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminReportResponse;
import org.example.QuanLyMuaVu.module.admin.repository.AdminReportReadRepository;
import org.example.QuanLyMuaVu.module.admin.repository.AdminReportReadRepository.SeasonFinancialRow;
import org.example.QuanLyMuaVu.module.financial.port.ExpenseQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {

    @Mock
    private ExpenseQueryPort expenseQueryPort;

    @Mock
    private AdminReportReadRepository adminReportReadRepository;

    @InjectMocks
    private AdminReportService adminReportService;

    @Test
    @DisplayName("getProfitReport aggregates revenue and cost with pending marketplace status")
    void getProfitReport_ComputesProfitAndPendingMarketplace() {
        when(adminReportReadRepository.findSeasonFinancialRows(any())).thenReturn(List.of(
                row(1, "S1", "Rice", "Farm A", "Plot A", "100", "500", "200")));

        List<AdminReportResponse.ProfitReport> rows = adminReportService.getProfitReport(AdminReportFilter.builder().build());

        assertEquals(1, rows.size());
        AdminReportResponse.ProfitReport row = rows.get(0);
        assertEquals(new BigDecimal("500"), row.getTotalRevenue());
        assertEquals(new BigDecimal("200"), row.getTotalExpense());
        assertEquals(new BigDecimal("300"), row.getGrossProfit());
        assertEquals(new BigDecimal("60.00"), row.getProfitMargin());
        assertEquals(new BigDecimal("150.00"), row.getReturnOnCost());
        assertNull(row.getMarketplaceRevenue());
        assertEquals("TODO_PENDING_MARKETPLACE_REVENUE_CONTRACT", row.getMarketplaceRevenueStatus());
    }

    @Test
    @DisplayName("getSummary aggregates totals and carries area filter with marketplace warning")
    void getSummary_AggregatesAndMarksMarketplacePending() {
        when(adminReportReadRepository.findSeasonFinancialRows(any())).thenReturn(List.of(
                row(1, "S1", "Rice", "Farm A", "Plot A", "100", "200", "40"),
                row(2, "S2", "Corn", "Farm B", "Plot B", "200", "300", "60")));

        AdminReportFilter filter = AdminReportFilter.builder()
                .areaMinHa(new BigDecimal("1.5"))
                .areaMaxHa(new BigDecimal("5.0"))
                .build();

        AdminReportResponse.ReportSummary summary = adminReportService.getSummary(filter);

        assertNotNull(summary);
        assertEquals(new BigDecimal("300"), summary.getActualYield());
        assertEquals(new BigDecimal("100"), summary.getTotalCost());
        assertEquals(new BigDecimal("500"), summary.getRevenue());
        assertEquals(new BigDecimal("400"), summary.getGrossProfit());
        assertEquals(new BigDecimal("80.00"), summary.getMarginPercent());
        assertEquals(new BigDecimal("0.33"), summary.getCostPerTon());
        assertNull(summary.getMarketplaceRevenue());
        assertEquals("TODO_PENDING_MARKETPLACE_REVENUE_CONTRACT", summary.getMarketplaceRevenueStatus());
        assertEquals(new BigDecimal("1.5"), summary.getAppliedFilters().getAreaMinHa());
        assertEquals(new BigDecimal("5.0"), summary.getAppliedFilters().getAreaMaxHa());
        assertTrue(summary.getWarnings().stream().anyMatch(msg -> msg.contains("Marketplace revenue is excluded")));
    }

    @Test
    @DisplayName("exportReportCsv for revenue includes marketplace contract columns")
    void exportReportCsv_RevenueIncludesMarketplaceColumns() {
        when(adminReportReadRepository.findSeasonFinancialRows(any())).thenReturn(List.of(
                row(3, "S3", "Coffee", "Farm C", "Plot C", "50", "250", "90")));

        String csv = adminReportService.exportReportCsv("revenue", AdminReportFilter.builder().build(), null);

        assertTrue(csv.startsWith(
                "crop_name,plot_name,total_quantity_kg,total_revenue,marketplace_revenue,marketplace_revenue_status,avg_price"));
        assertTrue(csv.contains("TODO_PENDING_MARKETPLACE_REVENUE_CONTRACT"));
    }

    private SeasonFinancialRow row(
            Integer seasonId,
            String seasonName,
            String cropName,
            String farmName,
            String plotName,
            String quantity,
            String revenue,
            String expense) {
        return SeasonFinancialRow.builder()
                .seasonId(seasonId)
                .seasonName(seasonName)
                .startDate(LocalDate.of(2026, 1, 1))
                .cropName(cropName)
                .farmName(farmName)
                .plotName(plotName)
                .harvestQuantityKg(new BigDecimal(quantity))
                .harvestRevenue(new BigDecimal(revenue))
                .totalExpense(new BigDecimal(expense))
                .build();
    }
}
