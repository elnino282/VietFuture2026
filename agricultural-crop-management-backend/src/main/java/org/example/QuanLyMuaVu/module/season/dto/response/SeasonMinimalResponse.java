package org.example.QuanLyMuaVu.module.season.dto.response;

import java.time.LocalDate;
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
public class SeasonMinimalResponse {
    Integer seasonId;
    String seasonName;
    LocalDate startDate;
    LocalDate endDate;
    LocalDate plannedHarvestDate;
}
