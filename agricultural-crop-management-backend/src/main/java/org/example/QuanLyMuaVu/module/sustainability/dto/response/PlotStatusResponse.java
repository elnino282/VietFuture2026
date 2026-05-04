package org.example.QuanLyMuaVu.module.sustainability.dto.response;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Plot status for dashboard Plot Status Map panel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlotStatusResponse {
    Integer plotId;
    String plotName;
    BigDecimal areaHa;
    String cropName;
    String stage;
    String health;
}
