package org.example.QuanLyMuaVu.module.season.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HarvestResponse {
    Integer id;
    Integer seasonId;
    String seasonName;
    LocalDate harvestDate;
    BigDecimal quantity;
    BigDecimal unit;
    String grade;
    BigDecimal revenue;
    String note;
    LocalDateTime createdAt;
}
