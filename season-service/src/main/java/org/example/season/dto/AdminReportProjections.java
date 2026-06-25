package org.example.season.dto;

import java.math.BigDecimal;

public class AdminReportProjections {

    public interface SeasonExpenseAgg {
        Integer getSeasonId();
        BigDecimal getTotalExpense();
    }

    public interface SeasonHarvestAgg {
        Integer getSeasonId();
        BigDecimal getTotalQuantity();
    }

    public interface SeasonHarvestCountAgg {
        Integer getSeasonId();
        Long getHarvestCount();
    }

    public interface SeasonRevenueAgg {
        Integer getSeasonId();
        BigDecimal getTotalQuantity();
        BigDecimal getTotalRevenue();
    }
}
