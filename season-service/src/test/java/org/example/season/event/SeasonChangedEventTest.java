package org.example.season.event;

import org.example.season.entity.Season;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeasonChangedEventTest {

    @Test
    void routingKey_Created_ShouldUseCreatedSuffix() {
        Season season = buildSeason();
        SeasonChangedEvent event = new SeasonChangedEvent(season, 1, SeasonChangedEvent.Action.CREATED);
        assertEquals("season.event.season.created", event.getEventType());
    }

    @Test
    void routingKey_Updated_ShouldUseUpdatedSuffix() {
        Season season = buildSeason();
        SeasonChangedEvent event = new SeasonChangedEvent(season, 1, SeasonChangedEvent.Action.UPDATED);
        assertEquals("season.event.season.updated", event.getEventType());
    }

    @Test
    void routingKey_StatusChanged_ShouldUseStatusDotChangedSuffix() {
        Season season = buildSeason();
        SeasonChangedEvent event = new SeasonChangedEvent(season, 1, SeasonChangedEvent.Action.STATUS_CHANGED);
        assertEquals("season.event.season.status.changed", event.getEventType());
    }

    @Test
    void routingKey_Completed_ShouldUseCompletedSuffix() {
        Season season = buildSeason();
        SeasonChangedEvent event = new SeasonChangedEvent(season, 1, SeasonChangedEvent.Action.COMPLETED);
        assertEquals("season.event.season.completed", event.getEventType());
    }

    private Season buildSeason() {
        return Season.builder()
                .id(5)
                .seasonName("Spring Rice")
                .plotId(2)
                .cropId(3)
                .varietyId(4)
                .startDate(LocalDate.of(2025, 3, 1))
                .plannedHarvestDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2025, 7, 1))
                .status(org.example.season.enums.SeasonStatus.PLANNED)
                .initialPlantCount(100)
                .currentPlantCount(100)
                .expectedYieldKg(new BigDecimal("1000"))
                .actualYieldKg(new BigDecimal("0"))
                .budgetAmount(new BigDecimal("5000"))
                .notes("Test season")
                .build();
    }
}
