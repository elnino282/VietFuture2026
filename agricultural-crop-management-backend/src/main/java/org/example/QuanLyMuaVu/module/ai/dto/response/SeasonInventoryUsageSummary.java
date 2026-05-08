package org.example.QuanLyMuaVu.module.ai.dto.response;

import java.math.BigDecimal;
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
public class SeasonInventoryUsageSummary {

    String itemName;
    String unit;
    BigDecimal totalOutQuantity;
    Integer movementCount;
}
