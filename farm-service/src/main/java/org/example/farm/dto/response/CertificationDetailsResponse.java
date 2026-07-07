package org.example.farm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificationDetailsResponse {
    private Integer recordId;
    private Integer farmId;
    private String standardCode;
    private String standardName;
    private BigDecimal complianceScore;
    private String status; // IN_PROGRESS, READY_TO_APPLY, APPLIED, CERTIFIED, REJECTED, EXPIRED
    private LocalDateTime appliedAt;
    private LocalDateTime certifiedAt;
    private LocalDate expiryDate;
    private String auditorNotes;
    private List<CertificationItemDetail> items;
    private Boolean isEligible;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificationItemDetail {
        private Integer id;
        private String itemCode;
        private String category;
        private String description;
        private Boolean isMandatory;
        private BigDecimal weightPct;
        private String dataSourceType;
        private String dataSourceQuery;
        private String status; // PASS, FAIL, PENDING, NOT_APPLICABLE
        private String evidenceUrl;
        private String notes;
        private LocalDateTime checkedAt;
    }
}
