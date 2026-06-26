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
public class CropChangedEventDto {
    private Integer cropId;
    private String cropName;
    private String description;
    private BigDecimal nContentKgPerKgYield;
    private String action;
}
