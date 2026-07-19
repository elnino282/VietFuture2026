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
public class CorrectiveActionResponse {
    Long id;
    Long nonconformityId;
    String planDescription;
    List<String> evidenceUrls;
    Integer appliesFromSeasonId;
    Long submittedByUserId;
    LocalDateTime submittedAt;
    Long reviewedByUserId;
    String reviewResult;
    String reviewNote;
    LocalDateTime reviewedAt;
    LocalDateTime createdAt;
}

