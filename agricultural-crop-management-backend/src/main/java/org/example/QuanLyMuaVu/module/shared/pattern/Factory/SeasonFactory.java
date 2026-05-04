package org.example.QuanLyMuaVu.module.shared.pattern.Factory;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.module.shared.pattern.Strategy.StatusTransitionStrategy;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateSeasonRequest;
import org.springframework.stereotype.Component;

/**
 * Factory Method Pattern: org.example.QuanLyMuaVu.module.season.entity.Season Factory.
 * <p>
 * Creates org.example.QuanLyMuaVu.module.season.entity.Season entities with proper defaults and validation.
 * Responsibilities:
 * - Generate season name if not provided
 * - Set initial status via Strategy pattern
 * - Populate audit fields
 * - Handle optional fields gracefully
 */
@Component
@RequiredArgsConstructor
public class SeasonFactory implements EntityFactory<org.example.QuanLyMuaVu.module.season.entity.Season, CreateSeasonRequest> {

    private final StatusTransitionStrategy<SeasonStatus> statusStrategy;

    @Override
    public org.example.QuanLyMuaVu.module.season.entity.Season create(CreateSeasonRequest request, org.example.QuanLyMuaVu.module.identity.entity.User creator) {
        org.example.QuanLyMuaVu.module.season.entity.Season season = new org.example.QuanLyMuaVu.module.season.entity.Season();

        // Generate season name if not provided
        String seasonName = request.getSeasonName();
        if (seasonName == null || seasonName.isBlank()) {
            seasonName = generateSeasonName(request.getStartDate());
        }
        season.setSeasonName(seasonName);

        // Use Strategy for initial status
        season.setStatus(statusStrategy.getInitialStatus());

        // Set dates
        season.setStartDate(request.getStartDate());
        season.setPlannedHarvestDate(request.getPlannedHarvestDate());
        season.setEndDate(request.getEndDate());

        // Set optional fields
        if (request.getExpectedYieldKg() != null) {
            season.setExpectedYieldKg(request.getExpectedYieldKg());
        }
        if (request.getInitialPlantCount() != null) {
            season.setInitialPlantCount(request.getInitialPlantCount());
            season.setCurrentPlantCount(request.getInitialPlantCount());
        }
        season.setNotes(request.getNotes());

        // Note: org.example.QuanLyMuaVu.module.season.entity.Season entity doesn't have createdBy field in current schema
        // If audit tracking is needed, consider adding it to the entity

        return season;
    }

    /**
     * Creates a org.example.QuanLyMuaVu.module.season.entity.Season with pre-fetched entities (from validation context).
     */
    public org.example.QuanLyMuaVu.module.season.entity.Season createWithEntities(
            CreateSeasonRequest request,
            org.example.QuanLyMuaVu.module.identity.entity.User creator,
            org.example.QuanLyMuaVu.module.farm.entity.Plot plot,
            org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop crop,
            org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety variety) {

        org.example.QuanLyMuaVu.module.season.entity.Season season = create(request, creator);
        season.setPlot(plot);
        season.setCrop(crop);
        season.setVariety(variety);
        return season;
    }

    /**
     * Generates a default season name based on the start date.
     * Format: "org.example.QuanLyMuaVu.module.season.entity.Season {Quarter} {Year}" (e.g., "org.example.QuanLyMuaVu.module.season.entity.Season Q1 2025")
     */
    private String generateSeasonName(LocalDate startDate) {
        if (startDate == null) {
            return "New org.example.QuanLyMuaVu.module.season.entity.Season";
        }

        int month = startDate.getMonthValue();
        int quarter = (month - 1) / 3 + 1;
        int year = startDate.getYear();

        return String.format("org.example.QuanLyMuaVu.module.season.entity.Season Q%d %d", quarter, year);
    }
}

