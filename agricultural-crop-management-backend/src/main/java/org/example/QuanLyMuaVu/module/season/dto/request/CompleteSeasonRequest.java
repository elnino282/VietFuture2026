package org.example.QuanLyMuaVu.module.season.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompleteSeasonRequest {
    /**
     * Required end date for the season.
     */
    @NotNull(message = "End date is required")
    LocalDate endDate;

    /**
     * Optional actual yield. If not provided, will be auto-calculated from
     * harvests.
     */
    BigDecimal actualYieldKg;

    /**
     * Set to true to force completion even if there are pending/in-progress tasks.
     * Default is false.
     */
    @Builder.Default
    Boolean forceComplete = false;
}
