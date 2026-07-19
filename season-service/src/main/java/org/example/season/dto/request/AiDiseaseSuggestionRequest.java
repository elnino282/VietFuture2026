package org.example.season.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AiDiseaseSuggestionRequest(
    @NotBlank(message = "Tên thu?c d? xu?t không du?c d? tr?ng")
    String suggestedPesticide,
    String diseaseName
) {}
