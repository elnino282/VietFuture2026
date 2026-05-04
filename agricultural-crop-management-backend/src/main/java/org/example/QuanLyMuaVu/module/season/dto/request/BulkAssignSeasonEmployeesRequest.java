package org.example.QuanLyMuaVu.module.season.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
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
public class BulkAssignSeasonEmployeesRequest {

    @NotEmpty(message = "KEY_INVALID")
    List<@NotNull(message = "KEY_INVALID") Long> employeeUserIds;

    BigDecimal wagePerTask;
}
