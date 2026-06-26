package org.example.sustainability.dto.event;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlotChangedEventDto {
    private Integer plotId;
    private Integer farmId;
    private String plotName;
    private BigDecimal area;
    private String soilType;
    private String boundaryGeoJson;
    private String status;
    private String action;
}
