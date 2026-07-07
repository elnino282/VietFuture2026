package org.example.marketplace.dto.client;

import java.time.LocalDate;

public record PesticideRecordDto(
        Integer id,
        Integer seasonId,
        String pesticideName,
        String activeIngredient,
        Integer phiDays,
        LocalDate harvestAllowedDate,
        LocalDate applicationDate) {
}
