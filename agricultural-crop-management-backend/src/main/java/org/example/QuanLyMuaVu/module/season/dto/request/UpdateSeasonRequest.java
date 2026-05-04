package org.example.QuanLyMuaVu.module.season.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class UpdateSeasonRequest {

    @NotBlank(message = "KEY_INVALID")
    String seasonName;

    @NotNull(message = "KEY_INVALID")
    LocalDate startDate;

    Integer varietyId;

    LocalDate plannedHarvestDate;

    /**
     * BR106: [dtpEndDate] - Mandatory end date field
     */
    @NotNull(message = "MSG_1")
    LocalDate endDate;

    @NotNull(message = "KEY_INVALID")
    @Min(value = 1, message = "KEY_INVALID")
    Integer currentPlantCount;

    BigDecimal expectedYieldKg;

    BigDecimal actualYieldKg;

    String notes;

    /**
     * BR106: [txtBoxDescription] - Optional description field
     */
    String description;
}
