package org.example.QuanLyMuaVu.module.ai.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeasonCostOptimizationSummaryResponse {

    Integer seasonId;
    String seasonName;
    BigDecimal budgetAmount;
    BigDecimal totalExpense;
    BigDecimal remainingBudget;
    List<SeasonCostCategoryBreakdown> expenseByCategory;
    List<SeasonCostCategoryBreakdown> topCostCategories;
    BigDecimal expectedYieldKg;
    BigDecimal actualYieldKg;
    BigDecimal costPerExpectedKg;
    BigDecimal costPerActualKg;
    BigDecimal laborCost;
    BigDecimal pesticideTreatmentCost;
    List<SeasonInventoryUsageSummary> inventoryUsageSummary;
    List<String> warnings;
    String disclaimer;
}
