package org.example.QuanLyMuaVu.module.cropcatalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class VarietyRequest {

    @NotNull(message = "KEY_INVALID")
    Integer cropId;

    @NotBlank(message = "KEY_INVALID")
    String name;

    String description;
}

