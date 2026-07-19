package org.example.delivery.dto.response;

import java.time.LocalDate;

public record BatchSuggestionResponse(
    boolean batchEligible,
    LocalDate suggestedDate,
    String zone,
    long currentCount,
    long threshold
) {}

