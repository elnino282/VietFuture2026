package org.example.farm.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificationAuditResponse {
    Long id;
    Integer recordId;
    String auditType;
    LocalDate scheduledDate;
    Long auditorUserId;
    String auditorOrgName;
    String status;
    String interviewNotes;
    String sampleCollectionNotes;
    LocalDateTime conductedAt;
    LocalDateTime createdAt;
}

