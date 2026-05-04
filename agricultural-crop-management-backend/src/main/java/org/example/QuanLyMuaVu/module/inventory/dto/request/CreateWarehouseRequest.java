package org.example.QuanLyMuaVu.module.inventory.dto.request;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateWarehouseRequest {

    @NotBlank(message = "Warehouse name is required")
    @Size(max = 150, message = "Warehouse name must not exceed 150 characters")
    String name;

    @NotNull(message = "Farm is required")
    Integer farmId;

    @NotBlank(message = "Warehouse type is required")
    String type;

    Integer provinceId;

    Integer wardId;
}
