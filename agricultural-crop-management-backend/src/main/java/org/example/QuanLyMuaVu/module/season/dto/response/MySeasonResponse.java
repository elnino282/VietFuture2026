package org.example.QuanLyMuaVu.module.season.dto.response;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Minimal season response for dropdown selectors.
 * Used by GET /api/v1/seasons/my
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MySeasonResponse {

    Integer seasonId;
    String seasonName;
    LocalDate startDate;
    LocalDate endDate;
    LocalDate plannedHarvestDate;
    String status;
}
