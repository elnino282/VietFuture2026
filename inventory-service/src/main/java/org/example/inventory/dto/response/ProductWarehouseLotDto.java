package org.example.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.inventory.enums.ProductWarehouseLotStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductWarehouseLotDto {
    private Integer id;
    private String lotCode;
    private Integer productId;
    private String productName;
    private String productVariant;
    private Integer harvestId;
    private Integer warehouseId;
    private Integer locationId;
    private LocalDate harvestedAt;
    private String unit;
    private BigDecimal initialQuantity;
    private BigDecimal onHandQuantity;
    private String grade;
    private String qualityStatus;
    private String note;
    private ProductWarehouseLotStatus status;
}
