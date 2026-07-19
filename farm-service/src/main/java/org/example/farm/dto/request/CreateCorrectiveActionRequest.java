package org.example.farm.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateCorrectiveActionRequest {
    String planDescription;
    List<String> evidenceUrls;
    Integer appliesFromSeasonId;  // nullable — mùa vụ tương lai
}

