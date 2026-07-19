package org.example.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyDocumentRequest {
    @NotBlank
    String status;  // VERIFIED or REJECTED

    String note;
}

