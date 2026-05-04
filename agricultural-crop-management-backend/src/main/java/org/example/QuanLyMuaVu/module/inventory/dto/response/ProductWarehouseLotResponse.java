package org.example.QuanLyMuaVu.module.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class ProductWarehouseLotResponse {

    Integer id;
    String lotCode;
    Integer productId;
    String productName;
    String productVariant;
    Integer seasonId;
    String seasonName;
    Integer farmId;
    String farmName;
    Integer plotId;
    String plotName;
    Integer harvestId;
    Integer warehouseId;
    String warehouseName;
    Integer locationId;
    String locationLabel;
    LocalDate harvestedAt;
    LocalDateTime receivedAt;
    String unit;
    BigDecimal initialQuantity;
    BigDecimal onHandQuantity;
    String grade;
    String qualityStatus;
    String traceabilityData;
    String note;
    String status;
    Long createdBy;
    String createdByName;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

