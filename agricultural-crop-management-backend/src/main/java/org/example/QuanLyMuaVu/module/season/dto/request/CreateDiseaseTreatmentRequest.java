package org.example.QuanLyMuaVu.module.season.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class CreateDiseaseTreatmentRequest {

    @NotNull(message = "KEY_INVALID")
    LocalDateTime treatedAt;

    @NotBlank(message = "KEY_INVALID")
    @Size(max = 100, message = "KEY_INVALID")
    String method;

    Integer supplyItemId;

    Integer supplyLotId;

    @Size(max = 150, message = "KEY_INVALID")
    String materialName;

    @DecimalMin(value = "0.0", inclusive = true, message = "KEY_INVALID")
    BigDecimal quantityUsed;

    @Size(max = 20, message = "KEY_INVALID")
    String unit;

    @DecimalMin(value = "0.0", inclusive = true, message = "KEY_INVALID")
    BigDecimal costAmount;

    Integer expenseId;

    String effectiveness;

    @Size(max = 4000, message = "KEY_INVALID")
    String resultSummary;

    LocalDate nextReviewAt;

    @Size(max = 4000, message = "KEY_INVALID")
    String notes;
}
