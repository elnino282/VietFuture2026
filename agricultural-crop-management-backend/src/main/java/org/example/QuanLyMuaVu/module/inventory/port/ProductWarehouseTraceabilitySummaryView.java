package org.example.QuanLyMuaVu.module.inventory.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProductWarehouseTraceabilitySummaryView(
        Integer lotId,
        String lotCode,
        FarmSource farm,
        PlotSource plot,
        SeasonSource season,
        HarvestSource harvest,
        LotSource lot,
        List<TransactionMilestone> milestones) {

    public record FarmSource(
            Integer id,
            String name) {
    }

    public record PlotSource(
            Integer id,
            String name) {
    }

    public record SeasonSource(
            Integer id,
            String name,
            LocalDate startDate,
            LocalDate plannedHarvestDate) {
    }

    public record HarvestSource(
            Integer id,
            LocalDate harvestedAt,
            BigDecimal quantity,
            String grade) {
    }

    public record LotSource(
            Integer id,
            String lotCode,
            String productName,
            String productVariant,
            LocalDate harvestedAt,
            LocalDateTime receivedAt,
            String unit,
            BigDecimal initialQuantity) {
    }

    public record TransactionMilestone(
            LocalDateTime occurredAt,
            String transactionType,
            String movementType,
            BigDecimal quantity,
            String unit,
            BigDecimal resultingOnHand) {
    }
}

