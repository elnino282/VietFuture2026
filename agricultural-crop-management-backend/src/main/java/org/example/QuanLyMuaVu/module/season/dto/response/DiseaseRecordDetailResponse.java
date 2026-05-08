package org.example.QuanLyMuaVu.module.season.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiseaseRecordDetailResponse {

    DiseaseRecordResponse record;
    List<DiseaseTreatmentResponse> treatments;
    Long treatmentCount;
    LocalDateTime latestTreatmentAt;
    BigDecimal totalTreatmentCost;
}
