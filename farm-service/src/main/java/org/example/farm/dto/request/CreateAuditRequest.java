package org.example.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAuditRequest {
    @NotBlank
    String auditType;  // INITIAL, PERIODIC, RE_AUDIT

    @NotNull
    LocalDate scheduledDate;

    Long auditorUserId;

    String auditorOrgName;
}

