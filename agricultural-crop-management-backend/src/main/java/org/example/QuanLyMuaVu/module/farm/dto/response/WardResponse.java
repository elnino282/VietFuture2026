package org.example.QuanLyMuaVu.module.farm.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO for ward data, used in address selection dropdowns.
 * Maps to the wards table in loc.sql.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WardResponse {
    Integer id;
    String name;
    String slug;
    String type;
    String nameWithType;
    Integer provinceId;
}
