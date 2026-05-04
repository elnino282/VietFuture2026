package org.example.QuanLyMuaVu.module.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
public class ProductWarehouseTraceabilityResponse {

    Integer lotId;
    String lotCode;
    String productName;
    String productVariant;
    Integer seasonId;
    String seasonName;
    Integer farmId;
    String farmName;
    Integer plotId;
    String plotName;
    Integer harvestId;
    LocalDate harvestedAt;
    LocalDateTime receivedAt;
    BigDecimal initialQuantity;
    BigDecimal onHandQuantity;
    String unit;
    Long recordedBy;
    String recordedByName;
    String traceabilityData;
    List<ProductWarehouseTransactionResponse> transactions;
}

