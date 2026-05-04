package org.example.QuanLyMuaVu.module.sustainability.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.NutrientInputSourceType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateIrrigationWaterAnalysisRequest {

    @NotNull(message = "KEY_INVALID")
    Integer plotId;

    @NotNull(message = "KEY_INVALID")
    LocalDate sampleDate;

    @DecimalMin(value = "0.0", inclusive = true, message = "KEY_INVALID")
    BigDecimal nitrateMgPerL;

    @DecimalMin(value = "0.0", inclusive = true, message = "KEY_INVALID")
    BigDecimal ammoniumMgPerL;

    @DecimalMin(value = "0.0", inclusive = true, message = "KEY_INVALID")
    BigDecimal totalNmgPerL;

    @NotNull(message = "KEY_INVALID")
    @DecimalMin(value = "0.0", inclusive = true, message = "KEY_INVALID")
    BigDecimal irrigationVolumeM3;

    @NotNull(message = "KEY_INVALID")
    NutrientInputSourceType sourceType;

    String sourceDocument;

    String labReference;

    String note;
}
