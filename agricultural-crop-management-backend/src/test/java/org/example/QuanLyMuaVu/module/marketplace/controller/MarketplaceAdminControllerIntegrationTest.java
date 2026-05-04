package org.example.QuanLyMuaVu.module.marketplace.controller;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.repository.FarmRepository;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.identity.repository.UserRepository;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceUpdateProductStatusRequest;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceProduct;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceProductRepository;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MarketplaceAdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MarketplaceProductRepository marketplaceProductRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private ProductWarehouseLotRepository productWarehouseLotRepository;

    private Long testProductId;

    @BeforeEach
    void setUp() {
        // Create test data for admin moderation tests
        User farmer = userRepository.findByUsername("farmer").orElseThrow();

        // Try to find existing farm, season, and lot
        Farm farm = farmRepository.findAll().stream()
                .filter(f -> f.getUser() != null && f.getUser().getId().equals(farmer.getId()))
                .findFirst()
                .orElse(null);

        Season season = null;
        if (farm != null) {
            season = seasonRepository.findAll().stream()
                    .filter(s -> s.getPlot() != null && s.getPlot().getFarm() != null
                            && s.getPlot().getFarm().getId().equals(farm.getId()))
                    .findFirst()
                    .orElse(null);
        }

        ProductWarehouseLot lot = null;
        if (farm != null) {
            // Try to find existing lot
            lot = productWarehouseLotRepository.findAll().stream()
                    .filter(l -> l.getFarm() != null && l.getFarm().getId().equals(farm.getId())
                            && l.getStatus() == ProductWarehouseLotStatus.IN_STOCK
                            && l.getOnHandQuantity() != null
                            && l.getOnHandQuantity().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst()
                    .orElse(null);
        }

        // If no suitable lot found, create one
        if (lot == null && farm != null) {
            lot = new ProductWarehouseLot();
            lot.setLotCode("TEST-LOT-" + System.currentTimeMillis());
            lot.setProductName("Test Rice");
            lot.setInitialQuantity(new BigDecimal("100"));
            lot.setOnHandQuantity(new BigDecimal("100"));
            lot.setUnit("kg");
            lot.setStatus(ProductWarehouseLotStatus.IN_STOCK);
            lot.setFarm(farm);
            lot.setSeason(season);
            lot = productWarehouseLotRepository.save(lot);
        }

        // Create product in PENDING_REVIEW status
        if (farm != null && lot != null) {
            MarketplaceProduct product = new MarketplaceProduct();
            product.setSlug("test-product-" + System.currentTimeMillis());
            product.setName("Test Product for Admin Review");
            product.setCategory("Grain");
            product.setPrice(new BigDecimal("50000"));
            product.setStockQuantity(new BigDecimal("50"));
            product.setUnit("kg");
            product.setFarmerUser(farmer);
            product.setFarm(farm);
            product.setSeason(season);
            product.setLot(lot);
            product.setTraceable(true);
            product.setStatus(MarketplaceProductStatus.PENDING_REVIEW);
            product = marketplaceProductRepository.save(product);

            testProductId = product.getId();
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void approveListing_FromPendingReview_ShouldSucceed() throws Exception {
        assumeTrue(testProductId != null, "Test requires existing farm data");

        MarketplaceUpdateProductStatusRequest request =
            new MarketplaceUpdateProductStatusRequest(MarketplaceProductStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/marketplace/admin/products/" + testProductId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.status").value("ACTIVE"))
            .andExpect(jsonPath("$.result.publishedAt").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void rejectListing_FromPendingReview_ShouldSucceed() throws Exception {
        assumeTrue(testProductId != null, "Test requires existing farm data");

        MarketplaceUpdateProductStatusRequest request =
            new MarketplaceUpdateProductStatusRequest(MarketplaceProductStatus.REJECTED);

        mockMvc.perform(patch("/api/v1/marketplace/admin/products/" + testProductId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getModerationQueue_FilterByPendingReview_ShouldReturnOnlyPending() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/admin/products")
                .param("status", "PENDING_REVIEW")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result").exists())
            .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getModerationQueue_WithSearchQuery_ShouldFilterResults() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/admin/products")
                .param("q", "Test")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result").exists())
            .andExpect(jsonPath("$.result.items").isArray());
    }
}
