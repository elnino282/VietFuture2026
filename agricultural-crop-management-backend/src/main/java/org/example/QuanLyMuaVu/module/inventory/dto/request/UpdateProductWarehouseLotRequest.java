package org.example.QuanLyMuaVu.module.inventory.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProductWarehouseLotRequest {

    String productName;

    String productVariant;

    Integer locationId;

    String grade;

    String qualityStatus;

    String traceabilityData;

    String note;

    String status;
}

