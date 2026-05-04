package org.example.QuanLyMuaVu.module.marketplace.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
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
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceAddressResponse;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceAddress;
import org.example.QuanLyMuaVu.module.marketplace.repository.*;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BuyerAddressIntegrationTest {

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

    private User buyer;

    @BeforeEach
    void setUp() {
        buyer = User.builder().id(10L).username("buyer").fullName("Buyer").build();
    }

    private MarketplaceAddressUpsertRequest req(String name, String phone, boolean def) {
        return new MarketplaceAddressUpsertRequest(name, phone, "HCM", "Q1", "P1", "Street", null, "home", def);
    }

    private MarketplaceAddress addr(Long id, User u, String name, boolean def) {
        return MarketplaceAddress.builder().id(id).user(u).fullName(name).phone("0901234567")
                .province("HCM").district("Q1").ward("P1").street("Street").label("home").isDefault(def).build();
    }

    @Test @DisplayName("First address auto-defaults")
    void createFirst_AutoDefault() {
        when(currentUserService.getCurrentUser()).thenReturn(buyer);
        when(marketplaceAddressRepository.existsByUser_IdAndDeletedAtIsNull(10L)).thenReturn(false);
        when(marketplaceAddressRepository.save(any())).thenAnswer(i -> { MarketplaceAddress a = i.getArgument(0); a.setId(1L); return a; });
        MarketplaceAddressResponse r = marketplaceService.createAddress(req("A", "0901234567", false));
        assertTrue(r.isDefault());
    }

    @Test @DisplayName("Second address not default")
    void createSecond_NotDefault() {
        when(currentUserService.getCurrentUser()).thenReturn(buyer);
        when(marketplaceAddressRepository.existsByUser_IdAndDeletedAtIsNull(10L)).thenReturn(true);
        when(marketplaceAddressRepository.save(any())).thenAnswer(i -> { MarketplaceAddress a = i.getArgument(0); a.setId(2L); return a; });
        MarketplaceAddressResponse r = marketplaceService.createAddress(req("B", "0987654321", false));
        assertFalse(r.isDefault());
    }

    @Test @DisplayName("Create with default=true clears others")
    void createWithDefault_Clears() {
        when(currentUserService.getCurrentUser()).thenReturn(buyer);
        when(marketplaceAddressRepository.clearDefaultByUserId(10L)).thenReturn(1);
        when(marketplaceAddressRepository.save(any())).thenAnswer(i -> { MarketplaceAddress a = i.getArgument(0); a.setId(3L); return a; });
        assertTrue(marketplaceService.createAddress(req("C", "0912345678", true)).isDefault());
    }

    @Test @DisplayName("List returns active addresses")
    void list_ReturnsActive() {
        when(currentUserService.getCurrentUserId()).thenReturn(10L);
        when(marketplaceAddressRepository.findAllByUser_IdAndDeletedAtIsNullOrderByIsDefaultDescIdDesc(10L))
                .thenReturn(List.of(addr(1L, buyer, "A", true), addr(2L, buyer, "B", false)));
        assertEquals(2, marketplaceService.listAddresses().size());
    }

    @Test @DisplayName("Update address fields")
    void update_Fields() {
        when(currentUserService.getCurrentUserId()).thenReturn(10L);
        when(marketplaceAddressRepository.findByIdAndUser_IdAndDeletedAtIsNull(1L, 10L)).thenReturn(Optional.of(addr(1L, buyer, "Old", false)));
        when(marketplaceAddressRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("New", marketplaceService.updateAddress(1L, req("New", "0987654321", false)).fullName());
    }

    @Test @DisplayName("Update non-existent throws NOT_FOUND")
    void update_NotFound() {
        when(currentUserService.getCurrentUserId()).thenReturn(10L);
        when(marketplaceAddressRepository.findByIdAndUser_IdAndDeletedAtIsNull(999L, 10L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.MARKETPLACE_ADDRESS_NOT_FOUND,
                assertThrows(AppException.class, () -> marketplaceService.updateAddress(999L, req("X", "0901234567", false))).getErrorCode());
    }

    @Test @DisplayName("Soft-delete sets deletedAt")
    void delete_SoftDeletes() {
        when(currentUserService.getCurrentUserId()).thenReturn(10L);
        MarketplaceAddress a = addr(1L, buyer, "A", false);
        when(marketplaceAddressRepository.findByIdAndUser_IdAndDeletedAtIsNull(1L, 10L)).thenReturn(Optional.of(a));
        when(marketplaceAddressRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        marketplaceService.deleteAddress(1L);
        assertNotNull(a.getDeletedAt());
    }

    @Test @DisplayName("Deleting default promotes next")
    void delete_Default_PromotesNext() {
        when(currentUserService.getCurrentUserId()).thenReturn(10L);
        MarketplaceAddress def = addr(1L, buyer, "Def", true);
        MarketplaceAddress next = addr(2L, buyer, "Next", false);
        when(marketplaceAddressRepository.findByIdAndUser_IdAndDeletedAtIsNull(1L, 10L)).thenReturn(Optional.of(def));
        when(marketplaceAddressRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(marketplaceAddressRepository.findAllByUser_IdAndDeletedAtIsNullOrderByIsDefaultDescIdDesc(10L)).thenReturn(List.of(next));
        marketplaceService.deleteAddress(1L);
        assertTrue(next.getIsDefault());
    }

    @Test @DisplayName("Set default clears all and sets target")
    void setDefault_Works() {
        when(currentUserService.getCurrentUserId()).thenReturn(10L);
        MarketplaceAddress a = addr(2L, buyer, "B", false);
        when(marketplaceAddressRepository.findByIdAndUser_IdAndDeletedAtIsNull(2L, 10L)).thenReturn(Optional.of(a));
        when(marketplaceAddressRepository.clearDefaultByUserId(10L)).thenReturn(1);
        when(marketplaceAddressRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertTrue(marketplaceService.setDefaultAddress(2L).isDefault());
    }

    @Test @DisplayName("Set default on missing throws NOT_FOUND")
    void setDefault_NotFound() {
        when(currentUserService.getCurrentUserId()).thenReturn(10L);
        when(marketplaceAddressRepository.findByIdAndUser_IdAndDeletedAtIsNull(999L, 10L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.MARKETPLACE_ADDRESS_NOT_FOUND,
                assertThrows(AppException.class, () -> marketplaceService.setDefaultAddress(999L)).getErrorCode());
    }
}
