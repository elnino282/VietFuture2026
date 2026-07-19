package org.example.cropcatalog.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class CropRequest {

    @NotBlank(message = "KEY_INVALID")
    String cropName;

    String description;\r\n\r\n    org.example.cropcatalog.entity.CropCategory category;\r\n\r\n    Integer postHarvestDelayDays;\r\n\r\n    Integer shelfLifeDays;\r\n\r\n    String defaultStorageCategory;\r\n\r\n    Boolean requiresColdChain;
}
