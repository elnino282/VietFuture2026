package org.example.QuanLyMuaVu.module.admin.dto.request;

import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request body for Admin to update a Season.
 * Used for admin intervention features like force-completing a season.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminSeasonUpdateRequest {

    /**
     * New status for the season.
     * Valid values: PLANNED, ACTIVE, COMPLETED, CANCELLED, ARCHIVED
     */
    String status;

    /**
     * End date of the season.
     * Required when status is COMPLETED.
     */
    LocalDate endDate;

    /**
     * Actual yield in kg.
     * Required when status is COMPLETED.
     */
    @Positive(message = "Actual yield must be positive")
    BigDecimal actualYieldKg;

    /**
     * Optional notes for the update.
     */
    String notes;
}
