package org.example.adminreporting.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeasonResponse {
    Integer id;
    String seasonName;
    Integer plotId;
    Integer cropId;
    Integer varietyId;
    LocalDate startDate;
    LocalDate plannedHarvestDate;
    LocalDate endDate;
    String status;
    BigDecimal expectedYieldKg;
    BigDecimal actualYieldKg;
}
