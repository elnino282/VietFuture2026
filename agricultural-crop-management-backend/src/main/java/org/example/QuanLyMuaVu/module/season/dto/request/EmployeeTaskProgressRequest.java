package org.example.QuanLyMuaVu.module.season.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class EmployeeTaskProgressRequest {

    @NotNull(message = "Progress percent is required")
    @Min(value = 0, message = "Progress percent must be greater than or equal to 0")
    @Max(value = 100, message = "Progress percent must be less than or equal to 100")
    Integer progressPercent;

    @Size(max = 4000, message = "Note must not exceed 4000 characters")
    String note;

    @Size(max = 1000, message = "Evidence URL must not exceed 1000 characters")
    String evidenceUrl;
}

