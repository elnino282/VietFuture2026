package org.example.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompleteAuditRequest {
    @NotBlank
    String result;  // PASSED or FAILED

    String interviewNotes;
    String sampleCollectionNotes;
}

