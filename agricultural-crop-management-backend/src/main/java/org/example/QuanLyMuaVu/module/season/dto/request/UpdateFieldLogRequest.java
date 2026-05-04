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
public class UpdateFieldLogRequest {

    @NotNull(message = "KEY_INVALID")
    LocalDate logDate;

    @NotBlank(message = "KEY_INVALID")
    @Size(max = 100, message = "KEY_INVALID")
    String logType;

    @Size(max = 4000, message = "KEY_INVALID")
    String notes;
}

