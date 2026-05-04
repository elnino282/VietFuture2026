package org.example.QuanLyMuaVu.module.farm.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO for province data, used in address selection dropdowns.
 * Maps to the provinces table in loc.sql.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProvinceResponse {
    Integer id;
    String name;
    String slug;
    String type;
    String nameWithType;
}
