package org.example.QuanLyMuaVu.module.admin.service;

import java.time.LocalDateTime;
import java.util.List;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminPendingApprovalItemDTO;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceOrder;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceProduct;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentVerificationStatus;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceOrderRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPendingApprovalServiceTest {

    @Mock
    MarketplaceProductRepository marketplaceProductRepository;
    @Mock
    MarketplaceOrderRepository marketplaceOrderRepository;

    @InjectMocks
    AdminPendingApprovalService adminPendingApprovalService;

    @Test
    @DisplayName("getPendingApprovals aggregates real pending products and payment proofs")
    void getPendingApprovals_aggregatesPendingItems() {
        User seller = User.builder().id(11L).username("sellerA").fullName("Seller A").build();
        Farm farm = Farm.builder().id(1).name("Farm One").build();
        MarketplaceProduct product = MarketplaceProduct.builder()
                .id(101L)
                .name("Rice Listing")
                .status(MarketplaceProductStatus.PENDING_REVIEW)
                .farmerUser(seller)
                .farm(farm)
                .updatedAt(LocalDateTime.of(2026, 5, 10, 8, 0))
                .build();

        User buyer = User.builder().id(22L).username("buyerA").build();
        MarketplaceOrder order = MarketplaceOrder.builder()
                .id(501L)
                .orderCode("ORD-501")
                .buyerUser(buyer)
                .paymentVerificationStatus(MarketplacePaymentVerificationStatus.SUBMITTED)
                .paymentProofUploadedAt(LocalDateTime.of(2026, 5, 9, 7, 30))
                .createdAt(LocalDateTime.of(2026, 5, 8, 9, 0))
                .build();

        when(marketplaceProductRepository.searchAdminProducts(
                eq(MarketplaceProductStatus.PENDING_REVIEW),
                eq(null),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(marketplaceOrderRepository.findByPaymentVerificationStatus(
                eq(MarketplacePaymentVerificationStatus.SUBMITTED),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        List<AdminPendingApprovalItemDTO> items = adminPendingApprovalService.getPendingApprovals(10);

        assertNotNull(items);
        assertEquals(2, items.size());
        assertEquals("PAYMENT_PROOF_VERIFICATION", items.get(0).getType());
        assertEquals("/admin/marketplace-orders?orderId=501", items.get(0).getActionUrl());
        assertEquals("MARKETPLACE_PRODUCT_REVIEW", items.get(1).getType());
        assertEquals("/admin/marketplace-products?status=PENDING_REVIEW", items.get(1).getActionUrl());
    }
}
