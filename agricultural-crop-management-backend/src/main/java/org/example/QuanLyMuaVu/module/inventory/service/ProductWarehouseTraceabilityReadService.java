package org.example.QuanLyMuaVu.module.inventory.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseTransactionType;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseTransaction;
import org.example.QuanLyMuaVu.module.inventory.port.ProductWarehouseTraceabilityReadPort;
import org.example.QuanLyMuaVu.module.inventory.port.ProductWarehouseTraceabilitySummaryView;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class ProductWarehouseTraceabilityReadService implements ProductWarehouseTraceabilityReadPort {

    ProductWarehouseLotRepository productWarehouseLotRepository;
    ProductWarehouseTransactionRepository productWarehouseTransactionRepository;

    @Override
    public Optional<ProductWarehouseTraceabilitySummaryView> findTraceabilitySummaryByLotId(Integer lotId) {
        if (lotId == null) {
            return Optional.empty();
        }
        return productWarehouseLotRepository.findById(lotId).map(this::toSummary);
    }

    private ProductWarehouseTraceabilitySummaryView toSummary(ProductWarehouseLot lot) {
        List<ProductWarehouseTraceabilitySummaryView.TransactionMilestone> milestones = productWarehouseTransactionRepository
                .findAllByLot_IdOrderByCreatedAtDesc(lot.getId())
                .stream()
                .sorted(Comparator.comparing(ProductWarehouseTransaction::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toMilestone)
                .toList();

        org.example.QuanLyMuaVu.module.farm.entity.Farm farm = lot.getFarm();
        org.example.QuanLyMuaVu.module.farm.entity.Plot plot = lot.getPlot();
        org.example.QuanLyMuaVu.module.season.entity.Season season = lot.getSeason();
        org.example.QuanLyMuaVu.module.season.entity.Harvest harvest = lot.getHarvest();

        ProductWarehouseTraceabilitySummaryView.FarmSource farmSource = farm == null
                ? null
                : new ProductWarehouseTraceabilitySummaryView.FarmSource(farm.getId(), farm.getName());
        ProductWarehouseTraceabilitySummaryView.PlotSource plotSource = plot == null
                ? null
                : new ProductWarehouseTraceabilitySummaryView.PlotSource(plot.getId(), plot.getPlotName());
        ProductWarehouseTraceabilitySummaryView.SeasonSource seasonSource = season == null
                ? null
                : new ProductWarehouseTraceabilitySummaryView.SeasonSource(
                season.getId(),
                season.getSeasonName(),
                season.getStartDate(),
                season.getPlannedHarvestDate());
        ProductWarehouseTraceabilitySummaryView.HarvestSource harvestSource = harvest == null
                ? null
                : new ProductWarehouseTraceabilitySummaryView.HarvestSource(
                harvest.getId(),
                harvest.getHarvestDate(),
                harvest.getQuantity(),
                harvest.getGrade());

        ProductWarehouseTraceabilitySummaryView.LotSource lotSource = new ProductWarehouseTraceabilitySummaryView.LotSource(
                lot.getId(),
                lot.getLotCode(),
                lot.getProductName(),
                lot.getProductVariant(),
                lot.getHarvestedAt(),
                lot.getReceivedAt(),
                lot.getUnit(),
                lot.getInitialQuantity());

        return new ProductWarehouseTraceabilitySummaryView(
                lot.getId(),
                lot.getLotCode(),
                farmSource,
                plotSource,
                seasonSource,
                harvestSource,
                lotSource,
                milestones);
    }

    private ProductWarehouseTraceabilitySummaryView.TransactionMilestone toMilestone(ProductWarehouseTransaction transaction) {
        return new ProductWarehouseTraceabilitySummaryView.TransactionMilestone(
                transaction.getCreatedAt(),
                transaction.getTransactionType() != null ? transaction.getTransactionType().name() : null,
                mapMovementType(transaction.getTransactionType()),
                transaction.getQuantity(),
                transaction.getUnit(),
                transaction.getResultingOnHand());
    }

    private String mapMovementType(ProductWarehouseTransactionType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case RECEIPT_FROM_HARVEST, RETURN, MARKETPLACE_ORDER_RELEASED -> "IN";
            case STOCK_OUT, MARKETPLACE_ORDER_RESERVED -> "OUT";
            case ADJUSTMENT, TRANSFER -> "ADJUST";
        };
    }
}

