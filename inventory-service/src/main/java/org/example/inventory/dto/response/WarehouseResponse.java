package org.example.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponse {
    private Integer id;
    private Integer farmId;
    private String farmName;
    private String name;
    private String type;
    private Integer provinceId;
    private Integer wardId;
}
