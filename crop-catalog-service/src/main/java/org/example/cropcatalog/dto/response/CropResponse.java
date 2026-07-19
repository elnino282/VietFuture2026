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
    String description;\r\n    org.example.cropcatalog.entity.CropCategory category;\r\n    Integer postHarvestDelayDays;\r\n    Integer shelfLifeDays;\r\n    String defaultStorageCategory;\r\n    Boolean requiresColdChain;
}
