package org.example.QuanLyMuaVu.module.farm.dto.response;

import java.math.BigDecimal;
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
public class FarmResponse {
    Integer id;
    String farmName;
    String name; // Alias for farmName (for frontend compatibility)
    String ownerUsername; // Owner's username (for admin views)
    Integer provinceId;
    String provinceName;
    Integer wardId;
    String wardName;
    BigDecimal area;
    Boolean active;
}
