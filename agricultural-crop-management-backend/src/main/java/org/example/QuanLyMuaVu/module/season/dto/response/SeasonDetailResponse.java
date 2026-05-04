package org.example.QuanLyMuaVu.module.season.dto.response;

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
public class SeasonDetailResponse {

    Integer id;
    String seasonName;
    Integer plotId;
    Integer cropId;
    Integer varietyId;
    String plotName;
    String cropName;
    String varietyName;
    LocalDate startDate;
    LocalDate plannedHarvestDate;
    LocalDate endDate;
    String status;
    Integer initialPlantCount;
    Integer currentPlantCount;
    BigDecimal expectedYieldKg;
    BigDecimal actualYieldKg;
    String notes;
}
