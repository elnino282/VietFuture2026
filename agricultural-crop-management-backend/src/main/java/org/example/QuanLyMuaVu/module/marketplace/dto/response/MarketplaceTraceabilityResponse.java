package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full buyer-facing traceability response for a marketplace product or purchased order item.
 * Each section is nullable — partial data is returned gracefully when upstream data is missing.
 */
public record MarketplaceTraceabilityResponse(
        Long productId,
        Boolean traceable,
        FarmTraceability farm,
        PlotTraceability plot,
        SeasonTraceability season,
        HarvestTraceability harvest,
        ProductLotTraceability productLot,
        List<TimelineMilestone> timeline,
        LocalDateTime validatedAt) {

    /**
     * Public-safe farm info. Never exposes internal costs, notes, or personal farmer data.
     *
     * @param certificationInfo TODO: Currently null — will be populated when the certification
     *                          feature is properly scoped and a DB column is added to the farms table.
     */
    public record FarmTraceability(
            Integer id,
            String name,
            String region,
            String address,
            String certificationInfo) {
    }

    public record PlotTraceability(
            Integer id,
            String name,
            BigDecimal area) {
    }

    public record SeasonTraceability(
            Integer id,
            String name,
            String cropName,
            String varietyName,
            LocalDate plantingDate,
            LocalDate harvestDate) {
    }

    /**
     * Harvest-lot level traceability. Uses {@code grade} for quality notes
     * (never the internal {@code note} field which may contain sensitive data).
     */
    public record HarvestTraceability(
            Integer id,
            LocalDate harvestDate,
            BigDecimal quantity,
            String qualityNotes) {
    }

    public record ProductLotTraceability(
            Integer id,
            String lotCode,
            LocalDate harvestedAt,
            LocalDateTime receivedAt,
            String unit,
            BigDecimal initialQuantity,
            String grade,
            String warehouseName,
            String storageLocation) {
    }

    /**
     * A single milestone in the product lifecycle timeline.
     * Sorted chronologically for easy frontend rendering.
     */
    public record TimelineMilestone(
            String milestone,
            LocalDateTime date,
            String description) {
    }
}
