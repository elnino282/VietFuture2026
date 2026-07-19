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

    String description;

    org.example.cropcatalog.entity.CropCategory category;

    Integer postHarvestDelayDays;

    Integer shelfLifeDays;

    String defaultStorageCategory;

    Boolean requiresColdChain;
}
