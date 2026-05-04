package org.example.QuanLyMuaVu.module.farm.dto.response;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.PlotStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlotResponse {
    Integer id;
    Integer farmId;
    String farmName;
    String plotName;
    BigDecimal area;
    String soilType;
    String boundaryGeoJson;
    PlotStatus status;
}
