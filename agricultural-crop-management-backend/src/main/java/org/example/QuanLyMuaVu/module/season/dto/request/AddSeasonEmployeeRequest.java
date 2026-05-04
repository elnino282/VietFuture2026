package org.example.QuanLyMuaVu.module.season.dto.request;



import jakarta.validation.constraints.DecimalMin;
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
public class AddSeasonEmployeeRequest {

    @NotNull(message = "Employee user ID is required")
    Long employeeUserId;

    @DecimalMin(value = "0", inclusive = true, message = "Wage per task must be greater than or equal to 0")
    BigDecimal wagePerTask;
}

