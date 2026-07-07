package org.example.farm.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FarmDocumentResponse(
    Integer id,
    Integer farmId,
    String documentType,
    String documentTypeLabel,  // label tiếng Việt
    String title,
    String description,
    String fileUrl,
    LocalDate issuedDate,
    LocalDate expiryDate,
    boolean isExpired,
    boolean isExpiringSoon,   // sắp hết hạn trong 30 ngày
    String verificationStatus,
    String verifiedByName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
