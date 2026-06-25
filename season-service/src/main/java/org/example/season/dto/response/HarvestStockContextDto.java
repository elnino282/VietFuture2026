package org.example.season.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HarvestStockContextDto {
    private String warehouseName;
    private Long matchingLots;
    private BigDecimal onHandQuantity;
    private String unit;
}
