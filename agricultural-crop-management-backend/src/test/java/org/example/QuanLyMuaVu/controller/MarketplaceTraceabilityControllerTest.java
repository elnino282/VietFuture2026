package org.example.QuanLyMuaVu.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.example.QuanLyMuaVu.module.marketplace.controller.MarketplaceBuyerOrderAliasController;
import org.example.QuanLyMuaVu.module.marketplace.controller.MarketplaceController;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceTraceabilityResponse;
import org.example.QuanLyMuaVu.module.marketplace.service.MarketplaceService;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer integration tests for Buyer Traceability (section 1.7).
 * <p>
 * Tests HTTP-level concerns: URL routing, security enforcement, JSON structure.
 * Service-layer logic is tested in {@code MarketplaceTraceabilityServiceTest}.
 */
@WebMvcTest({MarketplaceController.class, MarketplaceBuyerOrderAliasController.class})
@AutoConfigureMockMvc(addFilters = false) // disable security filters; security is tested separately below
class MarketplaceTraceabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarketplaceService marketplaceService;

    // Suppress any additional beans the controllers require
    @MockitoBean(name = "customJwtDecoder")
    private org.example.QuanLyMuaVu.module.identity.config.CustomJwtDecoder customJwtDecoder;

    private MarketplaceTraceabilityResponse buildFullResponse() {
        return new MarketplaceTraceabilityResponse(
                1L,
                true,
                new MarketplaceTraceabilityResponse.FarmTraceability(10, "Farm Alpha", "Đồng Tháp", "Tân Hồng, Đồng Tháp", null),
                new MarketplaceTraceabilityResponse.PlotTraceability(20, "Thửa A1", BigDecimal.valueOf(12.5)),
                new MarketplaceTraceabilityResponse.SeasonTraceability(30, "Vụ Đông Xuân", "Lúa", "Jasmine 85",
                        LocalDate.of(2026, 1, 15), LocalDate.of(2026, 4, 20)),
                new MarketplaceTraceabilityResponse.HarvestTraceability(40,
                        LocalDate.of(2026, 4, 18), BigDecimal.valueOf(5000), "A"),
                new MarketplaceTraceabilityResponse.ProductLotTraceability(50, "LOT-001",
                        LocalDate.of(2026, 4, 18), LocalDateTime.of(2026, 4, 19, 10, 0),
                        "kg", BigDecimal.valueOf(5000), "A", "Kho Chính", "Zone A, Aisle 3"),
                List.of(
                        new MarketplaceTraceabilityResponse.TimelineMilestone("PLANTED",
                                LocalDateTime.of(2026, 1, 15, 0, 0), "Crop planted — Lúa"),
                        new MarketplaceTraceabilityResponse.TimelineMilestone("TENDED",
                                LocalDateTime.of(2026, 1, 16, 0, 0), "Crop tended during growing season"),
                        new MarketplaceTraceabilityResponse.TimelineMilestone("HARVESTED",
                                LocalDateTime.of(2026, 4, 18, 0, 0), "Crop harvested — grade: A"),
                        new MarketplaceTraceabilityResponse.TimelineMilestone("STORED",
                                LocalDateTime.of(2026, 4, 19, 10, 0), "Stored in Kho Chính"),
                        new MarketplaceTraceabilityResponse.TimelineMilestone("LISTED",
                                LocalDateTime.of(2026, 4, 25, 14, 0), "Listed on marketplace")
                ),
                LocalDateTime.now());
    }

    private MarketplaceTraceabilityResponse buildPartialResponse() {
        return new MarketplaceTraceabilityResponse(
                2L, true,
                new MarketplaceTraceabilityResponse.FarmTraceability(10, "Farm Beta", "Cần Thơ", null, null),
                null, null, null,
                new MarketplaceTraceabilityResponse.ProductLotTraceability(50, "LOT-002",
                        LocalDate.of(2026, 3, 1), LocalDateTime.of(2026, 3, 2, 8, 0),
                        "kg", BigDecimal.valueOf(200), null, "Kho", null),
                List.of(
                        new MarketplaceTraceabilityResponse.TimelineMilestone("HARVESTED",
                                LocalDateTime.of(2026, 3, 1, 0, 0), "Crop harvested"),
                        new MarketplaceTraceabilityResponse.TimelineMilestone("STORED",
                                LocalDateTime.of(2026, 3, 2, 8, 0), "Stored in Kho")
                ),
                LocalDateTime.now());
    }

    // ─────────── Public product traceability ───────────

    @Nested
    @DisplayName("GET /api/v1/marketplace/products/{id}/traceability (public)")
    class PublicProductTraceability {

        @Test
        @DisplayName("Returns full traceability chain with all 5 levels and timeline")
        void fullChain() throws Exception {
            when(marketplaceService.getTraceability(1L)).thenReturn(buildFullResponse());

            mockMvc.perform(get("/api/v1/marketplace/products/1/traceability"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.productId").value(1))
                    .andExpect(jsonPath("$.result.traceable").value(true))
                    // Farm
                    .andExpect(jsonPath("$.result.farm.id").value(10))
                    .andExpect(jsonPath("$.result.farm.name").value("Farm Alpha"))
                    .andExpect(jsonPath("$.result.farm.region").value("Đồng Tháp"))
                    .andExpect(jsonPath("$.result.farm.certificationInfo").isEmpty())
                    // Plot
                    .andExpect(jsonPath("$.result.plot.id").value(20))
                    .andExpect(jsonPath("$.result.plot.name").value("Thửa A1"))
                    .andExpect(jsonPath("$.result.plot.area").value(12.5))
                    // Season
                    .andExpect(jsonPath("$.result.season.cropName").value("Lúa"))
                    .andExpect(jsonPath("$.result.season.varietyName").value("Jasmine 85"))
                    // Harvest
                    .andExpect(jsonPath("$.result.harvest.qualityNotes").value("A"))
                    // Product lot
                    .andExpect(jsonPath("$.result.productLot.lotCode").value("LOT-001"))
                    .andExpect(jsonPath("$.result.productLot.warehouseName").value("Kho Chính"))
                    .andExpect(jsonPath("$.result.productLot.storageLocation").value("Zone A, Aisle 3"))
                    // Timeline
                    .andExpect(jsonPath("$.result.timeline", hasSize(5)))
                    .andExpect(jsonPath("$.result.timeline[0].milestone").value("PLANTED"))
                    .andExpect(jsonPath("$.result.timeline[4].milestone").value("LISTED"))
                    // Validated timestamp
                    .andExpect(jsonPath("$.result.validatedAt").isNotEmpty());
        }

        @Test
        @DisplayName("Returns partial data when season/harvest missing (graceful fallback)")
        void partialData() throws Exception {
            when(marketplaceService.getTraceability(2L)).thenReturn(buildPartialResponse());

            mockMvc.perform(get("/api/v1/marketplace/products/2/traceability"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.farm.name").value("Farm Beta"))
                    .andExpect(jsonPath("$.result.plot").isEmpty())
                    .andExpect(jsonPath("$.result.season").isEmpty())
                    .andExpect(jsonPath("$.result.harvest").isEmpty())
                    .andExpect(jsonPath("$.result.productLot").isNotEmpty())
                    .andExpect(jsonPath("$.result.timeline", hasSize(2)));
        }

        @Test
        @DisplayName("Product not found returns 404")
        void notFound() throws Exception {
            when(marketplaceService.getTraceability(999L))
                    .thenThrow(new AppException(ErrorCode.MARKETPLACE_PRODUCT_NOT_FOUND));

            mockMvc.perform(get("/api/v1/marketplace/products/999/traceability"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ERR_MARKETPLACE_PRODUCT_NOT_FOUND"));
        }

        @Test
        @DisplayName("Legacy endpoint /traceability/{id} still works (backward compat)")
        void legacyEndpoint() throws Exception {
            when(marketplaceService.getTraceability(1L)).thenReturn(buildFullResponse());

            mockMvc.perform(get("/api/v1/marketplace/traceability/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.productId").value(1));
        }

        @Test
        @DisplayName("No auth required for public product traceability")
        void noAuthRequired() throws Exception {
            when(marketplaceService.getTraceability(1L)).thenReturn(buildFullResponse());

            // No auth header, security filters disabled — should succeed
            mockMvc.perform(get("/api/v1/marketplace/products/1/traceability"))
                    .andExpect(status().isOk());
        }
    }

    // ─────────── Buyer order-item traceability ───────────

    @Nested
    @DisplayName("GET /api/v1/buyer/orders/{orderId}/items/{itemId}/traceability")
    class BuyerOrderItemTraceability {

        @Test
        @DisplayName("Buyer gets traceability for own order item")
        void happyPath() throws Exception {
            when(marketplaceService.getOrderItemTraceability(100L, 500L))
                    .thenReturn(buildFullResponse());

            mockMvc.perform(get("/api/v1/buyer/orders/100/items/500/traceability"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.productId").value(1))
                    .andExpect(jsonPath("$.result.traceable").value(true))
                    .andExpect(jsonPath("$.result.farm").isNotEmpty())
                    .andExpect(jsonPath("$.result.timeline", hasSize(5)));
        }

        @Test
        @DisplayName("Wrong buyer gets ORDER_NOT_FOUND (data isolation)")
        void wrongBuyer() throws Exception {
            when(marketplaceService.getOrderItemTraceability(100L, 500L))
                    .thenThrow(new AppException(ErrorCode.MARKETPLACE_ORDER_NOT_FOUND));

            mockMvc.perform(get("/api/v1/buyer/orders/100/items/500/traceability"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ERR_MARKETPLACE_ORDER_NOT_FOUND"));
        }

        @Test
        @DisplayName("Item not in order returns ITEM_NOT_IN_ORDER")
        void itemNotInOrder() throws Exception {
            when(marketplaceService.getOrderItemTraceability(100L, 999L))
                    .thenThrow(new AppException(ErrorCode.MARKETPLACE_REVIEW_ITEM_NOT_IN_ORDER));

            mockMvc.perform(get("/api/v1/buyer/orders/100/items/999/traceability"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ERR_MARKETPLACE_REVIEW_ITEM_NOT_IN_ORDER"));
        }
    }

    // ─────────── Response does NOT expose sensitive data ───────────

    @Nested
    @DisplayName("Security: no sensitive data leaks")
    class SecurityNoLeaks {

        @Test
        @DisplayName("Response JSON does not contain sensitive field names")
        void noSensitiveFields() throws Exception {
            when(marketplaceService.getTraceability(1L)).thenReturn(buildFullResponse());

            String json = mockMvc.perform(get("/api/v1/marketplace/products/1/traceability"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // These fields should never appear in traceability responses
            org.assertj.core.api.Assertions.assertThat(json)
                    .doesNotContain("createdBy", "updatedBy", "traceabilityData",
                            "budgetAmount", "internalNote", "referenceType", "referenceId");
        }
    }
}
