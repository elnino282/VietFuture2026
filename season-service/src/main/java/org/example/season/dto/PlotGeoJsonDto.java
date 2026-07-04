package org.example.season.dto;

import lombok.Data;
import java.util.Map;

@Data
public class PlotGeoJsonDto {
    private Long plotId;
    private String plotName;
    private Map<String, Object> boundary; 
}
