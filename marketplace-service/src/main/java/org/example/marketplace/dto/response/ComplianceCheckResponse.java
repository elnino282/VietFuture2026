package org.example.marketplace.dto.response;

import java.util.List;

public record ComplianceCheckResponse(
        boolean isEligible,
        List<String> reasons,
        String complianceClaim,
        String certificationSnapshotJson,
        String harvestSafetySnapshotJson
) {
}

