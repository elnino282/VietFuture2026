package org.example.adminreporting.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportFilter {
    private Integer year;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer cropId;
    private Integer farmId;
    private Integer plotId;
    private Integer varietyId;
    private BigDecimal areaMinHa;
    private BigDecimal areaMaxHa;
    private Integer page;
    private Integer size;

    public LocalDate getEffectiveFromDate() {
        if (fromDate != null) {
            return fromDate;
        }
        if (year != null) {
            return LocalDate.of(year, 1, 1);
        }
        return null;
    }

    public LocalDate getEffectiveToDate() {
        if (toDate != null) {
            return toDate;
        }
        if (year != null) {
            return LocalDate.of(year + 1, 1, 1);
        }
        return null;
    }

    public boolean hasPagination() {
        return size != null && size > 0;
    }

    public int getSafePage() {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    public int getSafeSize() {
        if (!hasPagination()) {
            return 0;
        }
        return Math.min(size, 500);
    }

    public int getSafeOffset() {
        return getSafePage() * getSafeSize();
    }
}
