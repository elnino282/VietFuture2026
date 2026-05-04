package org.example.QuanLyMuaVu.module.admin.dto.request;

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

/**
 * Admin request DTO for updating a farm.
 * Uses PUT semantics - all fields are required for a complete replace.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminFarmUpdateRequest {

    @NotBlank(message = "Farm name is required")
    String name;

    @NotNull(message = "Province is required")
    Integer provinceId;

    @NotNull(message = "Ward is required")
    Integer wardId;

    @NotNull(message = "Area is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Area must be greater than 0")
    BigDecimal area;

    @NotNull(message = "Owner is required")
    Long ownerId;

    @NotNull(message = "Active status is required")
    Boolean active;
}
