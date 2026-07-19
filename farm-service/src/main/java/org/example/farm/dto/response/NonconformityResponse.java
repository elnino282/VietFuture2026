package org.example.farm.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NonconformityResponse {
    Long id;
    Long auditId;
    Integer checklistItemId;
    String severity;
    String description;
    String status;
    LocalDateTime createdAt;
    List<CorrectiveActionResponse> correctiveActions;
}

