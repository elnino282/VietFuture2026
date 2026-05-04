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
public class CreateSupplierRequest {

    @NotBlank(message = "Supplier name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    String name;

    @Size(max = 100, message = "License number must not exceed 100 characters")
    String licenseNo;

    @Size(max = 255, message = "Email must not exceed 255 characters")
    String contactEmail;

    @Size(max = 30, message = "Phone must not exceed 30 characters")
    String contactPhone;
}
