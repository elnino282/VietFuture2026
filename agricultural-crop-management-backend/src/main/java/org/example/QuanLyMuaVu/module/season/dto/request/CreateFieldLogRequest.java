package org.example.QuanLyMuaVu.module.season.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
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
public class CreateFieldLogRequest {

    @NotNull(message = "Season ID is required")
    Integer seasonId;

    @NotNull(message = "Log date is required")
    LocalDate logDate;

    @NotBlank(message = "Log type is required")
    @Size(max = 100, message = "Log type must not exceed 100 characters")
    String logType;

    @Size(max = 4000, message = "Notes must not exceed 4000 characters")
    String notes;
}
