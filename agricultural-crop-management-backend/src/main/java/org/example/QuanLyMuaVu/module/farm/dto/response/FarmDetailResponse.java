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
public class FarmDetailResponse {

    Integer id;
    String name;
    Integer provinceId;
    Integer wardId;
    String provinceName;
    String wardName;
    BigDecimal area;
    Boolean active;
    String ownerUsername;

    /**
     * Warning flag indicating if this farm has plots that may be inactive or
     * orphaned.
     * Only populated in restore operation responses.
     */
    Boolean hasOrphanedPlots;

    /**
     * Warning flag indicating if this farm has seasons that may be expired or
     * orphaned.
     * Only populated in restore operation responses.
     */
    Boolean hasOrphanedSeasons;
}
