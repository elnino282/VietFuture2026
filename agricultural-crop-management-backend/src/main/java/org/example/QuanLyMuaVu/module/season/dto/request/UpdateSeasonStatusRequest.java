package org.example.QuanLyMuaVu.module.season.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class UpdateSeasonStatusRequest {

    @NotBlank(message = "KEY_INVALID")
    String status;

    LocalDate actualStartDate;

    LocalDate actualEndDate;

    String reason;
}

