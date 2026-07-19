package org.example.cropcatalog.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CropResponse {
    Integer id;
    String cropName;
    String description;
    org.example.cropcatalog.entity.CropCategory category;
    Integer postHarvestDelayDays;
    Integer shelfLifeDays;
    String defaultStorageCategory;
    Boolean requiresColdChain;
}
