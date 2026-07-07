package org.example.season.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreatePesticideRecordRequest(
    @NotBlank(message = "Tên thuốc không được để trống")
    String pesticideName,
    String activeIngredient,
    Integer phiDays,
    @NotNull(message = "Ngày phun thuốc không được để trống")
    LocalDate applicationDate,
    String applicationMethod,
    String dosage,
    String targetPest,
    String note
) {}
