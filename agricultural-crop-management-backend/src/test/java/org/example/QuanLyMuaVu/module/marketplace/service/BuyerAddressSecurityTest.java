package org.example.QuanLyMuaVu.module.marketplace.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.example.QuanLyMuaVu.module.farm.repository.FarmRepository;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.incident.service.NotificationService;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseTransactionRepository;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceAddressUpsertRequest;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceAddress;
import org.example.QuanLyMuaVu.module.marketplace.repository.*;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Security tests: buyer cannot view/edit/delete another buyer's address.
 * The service layer enforces ownership via findByIdAndUser_Id queries.
 */
@ExtendWith(MockitoExtension.class)
class BuyerAddressSecurityTest {

    @Mock MarketplaceProductRepository marketplaceProductRepository;
    @Mock MarketplaceCartRepository marketplaceCartRepository;
    @Mock MarketplaceCartItemRepository marketplaceCartItemRepository;
    @Mock MarketplaceOrderGroupRepository marketplaceOrderGroupRepository;
    @Mock MarketplaceOrderRepository marketplaceOrderRepository;
    @Mock MarketplaceAddressRepository marketplaceAddressRepository;
    @Mock MarketplaceProductReviewRepository marketplaceProductReviewRepository;
    @Mock FarmRepository farmRepository;
    @Mock SeasonRepository seasonRepository;
    @Mock ProductWarehouseLotRepository productWarehouseLotRepository;
    @Mock ProductWarehouseTransactionRepository productWarehouseTransactionRepository;
    @Mock CurrentUserService currentUserService;
    @Mock ObjectMapper objectMapper;
    @Mock AppProperties appProperties;
    @Mock AuditLogService auditLogService;
    @Mock NotificationService notificationService;

    @InjectMocks MarketplaceService marketplaceService;

    private User buyerA;
    private User buyerB;

    @BeforeEach
    void setUp() {
        buyerA = User.builder().id(10L).username("buyerA").build();
        buyerB = User.builder().id(20L).username("buyerB").build();
    }

    private MarketplaceAddressUpsertRequest req() {
        return new MarketplaceAddressUpsertRequest("Name", "0901234567", "HCM", "Q1", "P1", "St", null, "home", false);
    }

    @Test
    @DisplayName("Buyer A cannot update Buyer B's address")
    void updateAddress_CrossBuyer_ThrowsNotFound() {
        // BuyerA is authenticated
        when(currentUserService.getCurrentUserId()).thenReturn(buyerA.getId());
        // Address 5 belongs to buyerB — query with buyerA's ID returns empty
        when(marketplaceAddressRepository.findByIdAndUser_IdAndDeletedAtIsNull(5L, buyerA.getId()))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> marketplaceService.updateAddress(5L, req()));
        assertEquals(ErrorCode.MARKETPLACE_ADDRESS_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("Buyer A cannot delete Buyer B's address")
    void deleteAddress_CrossBuyer_ThrowsNotFound() {
        when(currentUserService.getCurrentUserId()).thenReturn(buyerA.getId());
        when(marketplaceAddressRepository.findByIdAndUser_IdAndDeletedAtIsNull(5L, buyerA.getId()))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> marketplaceService.deleteAddress(5L));
        assertEquals(ErrorCode.MARKETPLACE_ADDRESS_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("Buyer A cannot set default on Buyer B's address")
    void setDefault_CrossBuyer_ThrowsNotFound() {
        when(currentUserService.getCurrentUserId()).thenReturn(buyerA.getId());
        when(marketplaceAddressRepository.findByIdAndUser_IdAndDeletedAtIsNull(5L, buyerA.getId()))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> marketplaceService.setDefaultAddress(5L));
        assertEquals(ErrorCode.MARKETPLACE_ADDRESS_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("List addresses only returns current user's addresses (implicit isolation)")
    void listAddresses_OnlyCurrentUser() {
        // BuyerA is authenticated — the service calls findAllByUser_Id with buyerA's ID
        when(currentUserService.getCurrentUserId()).thenReturn(buyerA.getId());
        when(marketplaceAddressRepository.findAllByUser_IdAndDeletedAtIsNullOrderByIsDefaultDescIdDesc(buyerA.getId()))
                .thenReturn(java.util.List.of());

        var result = marketplaceService.listAddresses();
        assertTrue(result.isEmpty());

        // Verify it was called with buyerA's ID, not buyerB's
        verify(marketplaceAddressRepository).findAllByUser_IdAndDeletedAtIsNullOrderByIsDefaultDescIdDesc(buyerA.getId());
        verify(marketplaceAddressRepository, never())
                .findAllByUser_IdAndDeletedAtIsNullOrderByIsDefaultDescIdDesc(buyerB.getId());
    }
}
