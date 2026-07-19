package org.example.marketplace.dto.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FarmCertificationDto(
        String certificationName,
        String certificationType,
        String status,
        LocalDate issuedDate,
        LocalDate expiryDate,
        BigDecimal complianceScore,
        String certificateNumber,
        Integer missingMandatoryEvidenceCount,
        java.util.List<MissingEvidenceItemDto> missingEvidenceItems
) {
    public record MissingEvidenceItemDto(
            String itemCode,
            String category,
            String description
    ) {}
}
