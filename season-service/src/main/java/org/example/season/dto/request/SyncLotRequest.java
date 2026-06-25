package org.example.season.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncLotRequest {
    private LocalDate harvestedAt;
    private BigDecimal initialQuantity;
    private BigDecimal onHandQuantity;
    private String grade;
    private String qualityStatus;
    private String note;
    private String status;
}
