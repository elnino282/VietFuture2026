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
public class IssueCertificateRequest {
    @NotBlank
    String certificateNumber;

    @NotNull
    LocalDate issuedDate;

    @NotNull
    LocalDate expiryDate;

    Integer certificateDocumentId;  // FK tới farm_documents.id
}

