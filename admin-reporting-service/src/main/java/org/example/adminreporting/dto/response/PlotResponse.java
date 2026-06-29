package org.example.adminreporting.dto.response;

import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
    String status;
}
