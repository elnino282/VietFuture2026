package org.example.QuanLyMuaVu.module.cropcatalog.dto.response;

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
}
