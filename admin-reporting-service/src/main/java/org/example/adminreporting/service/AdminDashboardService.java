package org.example.adminreporting.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminreporting.dto.response.AdminPendingApprovalItemDTO;
import org.example.adminreporting.dto.response.DashboardStatsDTO;
import org.example.adminreporting.entity.MarketplaceOrderSummary;
import org.example.adminreporting.entity.MarketplaceProductSummary;
import org.example.adminreporting.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserSummaryRepository userSummaryRepository;
    private final FarmSummaryRepository farmSummaryRepository;
    private final PlotSummaryRepository plotSummaryRepository;
    private final SeasonSummaryRepository seasonSummaryRepository;
    private final IncidentSummaryRepository incidentSummaryRepository;
    private final TaskSummaryRepository taskSummaryRepository;
    private final InventoryLotSummaryRepository inventoryLotSummaryRepository;
    private final MarketplaceOrderSummaryRepository marketplaceOrderSummaryRepository;
    private final MarketplaceProductSummaryRepository marketplaceProductSummaryRepository;

    public DashboardStatsDTO getDashboardStats() {
        log.info("Fetching admin dashboard stats");

        DashboardStatsDTO.Summary summary = DashboardStatsDTO.Summary.builder()
                .totalUsers(userSummaryRepository.count())
                .totalFarms(farmSummaryRepository.count())
                .totalPlots(plotSummaryRepository.count())
                .totalSeasons(seasonSummaryRepository.count())
                .build();

        DashboardStatsDTO.DataCoverage dataCoverage = DashboardStatsDTO.DataCoverage.builder()
                .incidentDataAvailable(incidentSummaryRepository.count() > 0)
                .taskDataAvailable(taskSummaryRepository.count() > 0)
                .build();

        return DashboardStatsDTO.builder()
                .summary(summary)
                .userRoleCounts(userSummaryRepository.countUsersByRole())
                .userStatusCounts(userSummaryRepository.countUsersByStatus())
                .seasonStatusCounts(seasonSummaryRepository.countSeasonsByStatus())
                .riskySeasons(getRiskySeasons(10))
                .dataCoverage(dataCoverage)
                .inventoryHealth(inventoryLotSummaryRepository.findInventoryHealth(LocalDate.now(), LocalDate.now().plusDays(30)))
                .unavailableReasons(buildUnavailableReasons(dataCoverage))
                .build();
    }

    public List<AdminPendingApprovalItemDTO> getPendingApprovals(Integer limit) {
        int resolvedLimit = limit == null || limit < 1 ? 10 : Math.min(limit, 100);
        List<AdminPendingApprovalItemDTO> items = new ArrayList<>();

        Pageable pageable = PageRequest.of(0, resolvedLimit, Sort.by(Sort.Order.asc("updatedAt"), Sort.Order.asc("productId")));
        List<MarketplaceProductSummary> pendingProducts = marketplaceProductSummaryRepository.findByStatus("PENDING_REVIEW", pageable);
        pendingProducts.forEach(product -> items.add(toPendingProductApproval(product)));

        Pageable orderPageable = PageRequest.of(0, resolvedLimit, Sort.by(Sort.Order.asc("paymentProofUploadedAt"), Sort.Order.asc("orderId")));
        List<MarketplaceOrderSummary> pendingOrders = marketplaceOrderSummaryRepository.findByPaymentStatus("SUBMITTED", orderPageable);
        pendingOrders.forEach(order -> items.add(toPendingPaymentProofApproval(order)));

        return items.stream()
                .sorted(Comparator
                        .comparingInt(this::priorityRank)
                        .thenComparing(
                                AdminPendingApprovalItemDTO::getSubmittedAt,
                                Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(AdminPendingApprovalItemDTO::getId, Comparator.nullsLast(Long::compareTo)))
                .limit(resolvedLimit)
                .toList();
    }

    private List<DashboardStatsDTO.RiskySeason> getRiskySeasons(int limit) {
        return seasonSummaryRepository.findRiskySeasonsRaw().stream()
                .map(this::enrichAndCalculateRisk)
                .filter(p -> p.getRiskScore() > 0)
                .sorted(Comparator.comparing(DashboardStatsDTO.RiskySeason::getRiskScore).reversed()
                        .thenComparing(DashboardStatsDTO.RiskySeason::getSeasonId, Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    private DashboardStatsDTO.RiskySeason enrichAndCalculateRisk(DashboardStatsDTO.RiskySeason riskySeason) {
        List<DashboardStatsDTO.RiskBasis> riskBasis = new ArrayList<>();
        long score = 0;

        if (riskySeason.getIncidentCount() != null && riskySeason.getIncidentCount() > 0) {
            riskBasis.add(DashboardStatsDTO.RiskBasis.OPEN_INCIDENTS);
            score += riskySeason.getIncidentCount();
        }
        if (riskySeason.getOverdueTaskCount() != null && riskySeason.getOverdueTaskCount() > 0) {
            riskBasis.add(DashboardStatsDTO.RiskBasis.OVERDUE_TASKS);
            score += riskySeason.getOverdueTaskCount();
        }
        if (riskySeason.getHighFdnRiskCount() != null && riskySeason.getHighFdnRiskCount() > 0) {
            riskBasis.add(DashboardStatsDTO.RiskBasis.HIGH_FDN_RISK);
            score += 1;
        }
        if (riskySeason.getInventoryRiskCount() != null && riskySeason.getInventoryRiskCount() > 0) {
            riskBasis.add(DashboardStatsDTO.RiskBasis.INVENTORY_RISK);
            score += 1;
        }

        riskySeason.setRiskBasis(riskBasis);
        riskySeason.setRiskScore(score);
        return riskySeason;
    }

    private List<String> buildUnavailableReasons(DashboardStatsDTO.DataCoverage dataCoverage) {
        List<String> reasons = new ArrayList<>();
        if (dataCoverage == null || !dataCoverage.isIncidentDataAvailable()) {
            reasons.add("INCIDENT_DATA_UNAVAILABLE");
        }
        if (dataCoverage == null || !dataCoverage.isTaskDataAvailable()) {
            reasons.add("TASK_DATA_UNAVAILABLE");
        }
        return reasons;
    }

    private AdminPendingApprovalItemDTO toPendingProductApproval(MarketplaceProductSummary product) {
        String farmName = product.getFarmName() != null ? product.getFarmName().trim() : null;
        String sellerName = product.getFarmerName() != null ? product.getFarmerName().trim() : null;

        List<String> subtitleParts = new ArrayList<>();
        if (farmName != null && !farmName.isEmpty()) {
            subtitleParts.add("Farm: " + farmName);
        }
        if (sellerName != null && !sellerName.isEmpty()) {
            subtitleParts.add("Seller: " + sellerName);
        } else if (product.getFarmerId() != null) {
            subtitleParts.add("Seller user #" + product.getFarmerId());
        }
        if (subtitleParts.isEmpty()) {
            subtitleParts.add("Listing #" + product.getProductId());
        }
        String subtitle = String.join(" | ", subtitleParts);

        String productTitle = product.getProductName() != null ? product.getProductName().trim() : ("Listing #" + product.getProductId());

        return AdminPendingApprovalItemDTO.builder()
                .id(product.getProductId())
                .type("MARKETPLACE_PRODUCT_REVIEW")
                .title("Review listing: " + productTitle)
                .subtitle(subtitle)
                .submittedAt(product.getUpdatedAt())
                .priority("MEDIUM")
                .severity("MEDIUM")
                .actionUrl("/admin/marketplace-products?status=PENDING_REVIEW")
                .actionTarget("MARKETPLACE_PRODUCT_MODERATION")
                .build();
    }

    private AdminPendingApprovalItemDTO toPendingPaymentProofApproval(MarketplaceOrderSummary order) {
        String orderCode = order.getOrderCode() != null ? order.getOrderCode().trim() : null;
        String orderLabel = orderCode != null ? orderCode : ("#" + order.getOrderId());
        String buyerLabel = order.getBuyerId() != null ? ("Buyer #" + order.getBuyerId()) : null;
        List<String> subtitleParts = new ArrayList<>();
        subtitleParts.add("Order " + orderLabel);
        if (buyerLabel != null) {
            subtitleParts.add(buyerLabel);
        }

        String subtitle = String.join(" | ", subtitleParts);

        return AdminPendingApprovalItemDTO.builder()
                .id(order.getOrderId())
                .type("PAYMENT_PROOF_VERIFICATION")
                .title("Verify payment proof")
                .subtitle(subtitle)
                .submittedAt(order.getPaymentProofUploadedAt() != null ? order.getPaymentProofUploadedAt() : order.getCreatedAt())
                .priority("HIGH")
                .severity("HIGH")
                .actionUrl("/admin/marketplace-orders?orderId=" + order.getOrderId())
                .actionTarget("PAYMENT_PROOF_VERIFICATION")
                .build();
    }

    private int priorityRank(AdminPendingApprovalItemDTO item) {
        String severity = item.getSeverity() != null
                ? item.getSeverity().trim().toUpperCase(Locale.ROOT)
                : "";
        return switch (severity) {
            case "CRITICAL" -> 0;
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 4;
        };
    }
}
