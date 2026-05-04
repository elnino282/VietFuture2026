package org.example.QuanLyMuaVu.module.inventory.dto.response;

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
public class WarehouseResponse {

    Integer id;
    String name;
    String type;
    Integer farmId;
    String farmName;
    Integer provinceId;
    Integer wardId;
}
