package org.example.QuanLyMuaVu.module.admin.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminPendingApprovalItemDTO;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrder;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceProduct;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentVerificationStatus;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceOrderRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPendingApprovalService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private final MarketplaceProductRepository marketplaceProductRepository;
    private final MarketplaceOrderRepository marketplaceOrderRepository;

    public List<AdminPendingApprovalItemDTO> getPendingApprovals(Integer limit) {
        int resolvedLimit = normalizeLimit(limit);
        List<AdminPendingApprovalItemDTO> items = new ArrayList<>();

        Pageable productPage = PageRequest.of(
                0,
                resolvedLimit,
                Sort.by(Sort.Order.asc("updatedAt"), Sort.Order.asc("id")));
        List<MarketplaceProduct> pendingProducts = marketplaceProductRepository
                .searchAdminProducts(MarketplaceProductStatus.PENDING_REVIEW, null, productPage)
                .getContent();
        pendingProducts.forEach(product -> items.add(toPendingProductApproval(product)));

        Pageable paymentProofPage = PageRequest.of(
                0,
                resolvedLimit,
                Sort.by(Sort.Order.asc("paymentProofUploadedAt"), Sort.Order.asc("id")));
        List<MarketplaceOrder> pendingPaymentProofs = marketplaceOrderRepository
                .findByPaymentVerificationStatus(MarketplacePaymentVerificationStatus.SUBMITTED, paymentProofPage)
                .getContent();
        pendingPaymentProofs.forEach(order -> items.add(toPendingPaymentProofApproval(order)));

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

    private AdminPendingApprovalItemDTO toPendingProductApproval(MarketplaceProduct product) {
        String farmName = product.getFarm() != null ? safeText(product.getFarm().getName()) : null;
        String sellerName = product.getFarmerUser() != null
                ? firstNonBlank(
                        product.getFarmerUser().getFullName(),
                        product.getFarmerUser().getUsername(),
                        product.getFarmerUser().getEmail())
                : null;

        List<String> subtitleParts = new ArrayList<>();
        if (farmName != null) {
            subtitleParts.add("Farm: " + farmName);
        }
        if (sellerName != null) {
            subtitleParts.add("Seller: " + sellerName);
        } else if (product.getFarmerUser() != null && product.getFarmerUser().getId() != null) {
            subtitleParts.add("Seller user #" + product.getFarmerUser().getId());
        }
        if (subtitleParts.isEmpty()) {
            subtitleParts.add("Listing #" + product.getId());
        }
        String subtitle = String.join(" | ", subtitleParts);

        String productTitle = safeText(product.getName());
        if (productTitle == null) {
            productTitle = "Listing #" + product.getId();
        }

        return AdminPendingApprovalItemDTO.builder()
                .id(product.getId())
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

    private AdminPendingApprovalItemDTO toPendingPaymentProofApproval(MarketplaceOrder order) {
        String orderCode = safeText(order.getOrderCode());
        String orderLabel = orderCode != null ? orderCode : ("#" + order.getId());
        String buyerLabel = order.getBuyerUser() != null && order.getBuyerUser().getId() != null
                ? ("Buyer #" + order.getBuyerUser().getId())
                : null;
        List<String> subtitleParts = new ArrayList<>();
        subtitleParts.add("Order " + orderLabel);
        if (buyerLabel != null) {
            subtitleParts.add(buyerLabel);
        }

        String subtitle = String.join(" | ", subtitleParts);

        return AdminPendingApprovalItemDTO.builder()
                .id(order.getId())
                .type("PAYMENT_PROOF_VERIFICATION")
                .title("Verify payment proof")
                .subtitle(subtitle)
                .submittedAt(order.getPaymentProofUploadedAt() != null
                        ? order.getPaymentProofUploadedAt()
                        : order.getCreatedAt())
                .priority("HIGH")
                .severity("HIGH")
                .actionUrl("/admin/marketplace-orders?orderId=" + order.getId())
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

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static String safeText(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null) {
                String normalized = value.trim();
                if (!normalized.isEmpty()) {
                    return normalized;
                }
            }
        }
        return null;
    }
}
