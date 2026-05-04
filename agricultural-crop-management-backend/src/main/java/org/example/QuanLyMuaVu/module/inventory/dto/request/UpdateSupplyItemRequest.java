package org.example.QuanLyMuaVu.module.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class UpdateSupplyItemRequest {

    @NotBlank(message = "Supply item name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    String name;

    @Size(max = 30, message = "Category must not exceed 30 characters")
    String category;

    @Size(max = 150, message = "Active ingredient must not exceed 150 characters")
    String activeIngredient;

    @Size(max = 20, message = "Unit must not exceed 20 characters")
    String unit;

    Boolean restrictedFlag;

    String description;
}
