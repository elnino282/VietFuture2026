package org.example.adminreporting.dto.response;

import java.math.BigDecimal;
import lombok.*;
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
    BigDecimal latitude;
    BigDecimal longitude;
    Boolean active;
}
