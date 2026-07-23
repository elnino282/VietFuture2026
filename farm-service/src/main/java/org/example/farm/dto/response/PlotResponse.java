package org.example.farm.dto.response;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.farm.enums.PlotStatus;
import org.locationtech.jts.geom.Geometry;

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
    Integer parentPlotId;
    @JsonIgnore
    Geometry polygon;
    PlotStatus status;
}
