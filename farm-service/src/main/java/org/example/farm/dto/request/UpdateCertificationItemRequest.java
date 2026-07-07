package org.example.farm.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCertificationItemRequest {
    private String status; // PASS, FAIL, PENDING, NOT_APPLICABLE
    private String evidenceUrl;
    private String notes;
}
