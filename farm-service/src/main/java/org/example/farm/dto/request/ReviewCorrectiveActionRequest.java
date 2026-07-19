package org.example.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewCorrectiveActionRequest {
    @NotBlank
    String result;  // ACCEPTED or REJECTED

    String reviewNote;
}

