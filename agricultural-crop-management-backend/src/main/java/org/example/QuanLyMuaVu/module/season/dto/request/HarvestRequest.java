package org.example.QuanLyMuaVu.module.season.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HarvestRequest {
    Integer seasonId;
    LocalDate harvestDate;
    BigDecimal quantity;
    BigDecimal unit;
    String grade;
    String note;
}
