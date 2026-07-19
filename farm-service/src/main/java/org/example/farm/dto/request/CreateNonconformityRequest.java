package org.example.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateNonconformityRequest {
    Integer checklistItemId;  // nullable — liên kết tiêu chí cụ thể nếu có

    @NotBlank
    String severity;  // MINOR, MAJOR, CRITICAL

    @NotBlank
    String description;
}

