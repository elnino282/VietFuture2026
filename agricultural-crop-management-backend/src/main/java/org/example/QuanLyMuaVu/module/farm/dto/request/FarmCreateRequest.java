package org.example.QuanLyMuaVu.module.farm.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class FarmCreateRequest {

    @NotBlank(message = "Farm name is required")
    String farmName;

    @NotNull(message = "Province ID is required")
    Integer provinceId;

    @NotNull(message = "Ward ID is required")
    Integer wardId;

    @NotNull(message = "Area is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Area must be greater than 0")
    BigDecimal area;

    @Builder.Default
    Boolean active = true;
}
