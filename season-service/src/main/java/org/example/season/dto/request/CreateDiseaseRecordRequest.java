package org.example.season.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
public class CreateDiseaseRecordRequest {

    @NotBlank(message = "KEY_INVALID")
    @Size(max = 150, message = "KEY_INVALID")
    String diseaseName;

    @Size(max = 4000, message = "KEY_INVALID")
    String symptomSummary;

    @NotBlank(message = "KEY_INVALID")
    String severity;

    @Size(max = 30, message = "KEY_INVALID")
    String status;

    @NotNull(message = "KEY_INVALID")
    LocalDateTime detectedAt;

    @DecimalMin(value = "0", inclusive = true, message = "KEY_INVALID")
    Integer affectedPlantCount;

    @DecimalMin(value = "0.0", inclusive = true, message = "KEY_INVALID")
    BigDecimal affectedAreaValue;

    @Size(max = 20, message = "KEY_INVALID")
    String affectedAreaUnit;

    @Size(max = 1000, message = "KEY_INVALID")
    String evidenceUrl;

    @Size(max = 4000, message = "KEY_INVALID")
    String notes;

    Integer incidentId;
}
