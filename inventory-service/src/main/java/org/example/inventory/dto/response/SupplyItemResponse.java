package org.example.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyItemResponse {
    private Integer id;
    private String name;
    private String activeIngredient;
    private String unit;
    private Boolean restrictedFlag;
}
