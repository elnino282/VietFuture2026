package org.example.season.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonReportDto {
    private String seasonId;
    private BigDecimal totalYieldKg;
    private BigDecimal expectedYieldKg;
    private BigDecimal totalExpenseVnd;
    private List<ExpenseCategoryDto> expenseByCategory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseCategoryDto {
        private String category;
        private BigDecimal amountVnd;
    }
}
