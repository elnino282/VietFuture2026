package org.example.QuanLyMuaVu.module.sustainability.dto.request;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmerReportFilter {

    private Integer seasonId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer cropId;
    private Integer farmId;
    private Integer plotId;
    private Integer page;
    private Integer size;

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
        return Math.min(size, 200);
    }
}
