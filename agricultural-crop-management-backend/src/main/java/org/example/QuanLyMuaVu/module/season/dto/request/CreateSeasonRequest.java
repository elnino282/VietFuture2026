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
public class CreateSeasonRequest {

    @NotNull(message = "KEY_INVALID")
    Integer plotId;

    @NotNull(message = "KEY_INVALID")
    Integer cropId;

    Integer varietyId;

    @NotBlank(message = "KEY_INVALID")
    String seasonName;

    @NotNull(message = "KEY_INVALID")
    LocalDate startDate;

    LocalDate plannedHarvestDate;

    /**
     * BR102: [dtpEndDate] - Mandatory end date field
     */
    @NotNull(message = "MSG_1")
    LocalDate endDate;

    @NotNull(message = "KEY_INVALID")
    @Min(value = 1, message = "KEY_INVALID")
    Integer initialPlantCount;

    BigDecimal expectedYieldKg;

    String notes;

    /**
     * BR102: [txtBoxDescription] - Optional description field
     */
    String description;
}
