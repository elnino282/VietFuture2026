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
public class StockLocationResponse {

    Integer id;
    Integer warehouseId;
    String zone;
    String aisle;
    String shelf;
    String bin;
    String label; // Computed: Z1-A1-S1-B1
}
