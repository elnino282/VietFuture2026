package org.example.farm.dto.response;

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
    String name; 
    String ownerUsername; 
    Long userId;
    Integer provinceId;
    String provinceName;
    Integer wardId;
    String wardName;
    BigDecimal area;
    BigDecimal latitude;
    BigDecimal longitude;
    Boolean active;
}
