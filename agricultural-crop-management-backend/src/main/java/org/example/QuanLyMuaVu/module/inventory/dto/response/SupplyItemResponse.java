package org.example.QuanLyMuaVu.module.inventory.dto.response;

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
public class SupplyItemResponse {

    Integer id;
    String name;
    String activeIngredient;
    String unit;
    Boolean restrictedFlag;
}
