package org.example.QuanLyMuaVu.module.marketplace.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.farm.entity.Province;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import org.example.QuanLyMuaVu.module.farm.repository.FarmRepository;
import org.example.QuanLyMuaVu.module.farm.repository.PlotRepository;
import org.example.QuanLyMuaVu.module.farm.repository.ProvinceRepository;
import org.example.QuanLyMuaVu.module.farm.repository.WardRepository;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.identity.repository.UserRepository;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.inventory.entity.Warehouse;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.WarehouseRepository;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceCart;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceCartItem;
import org.example.QuanLyMuaVu.module.marketplace.entity.MarketplaceProduct;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceCartItemRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceCartRepository;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MarketplaceBuyerCartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private PlotRepository plotRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductWarehouseLotRepository productWarehouseLotRepository;

    @Autowired
    private MarketplaceProductRepository marketplaceProductRepository;

    @Autowired
    private MarketplaceCartRepository marketplaceCartRepository;

    @Autowired
    private MarketplaceCartItemRepository marketplaceCartItemRepository;

    /**
     * Test user ID 4L corresponds to the "buyer" test account created by ApplicationInitConfig.
     * This user has BUYER role and is used consistently across cart tests to ensure proper
     * cart state isolation and predictable test behavior.
     */
    private static final Long TEST_BUYER_USER_ID = 4L;

    @BeforeEach
    void setUp() {
        // Tests use @Transactional to ensure clean state between test runs.
        // TEST_BUYER_USER_ID (4L) is the default "buyer" account from ApplicationInitConfig.
        // Each test creates its own cart state which is rolled back after test completion.
    }

    @Test
    void getCart_WhenEmpty_ShouldReturnEmptyCart() throws Exception {
        // Use a user ID that has never created a cart
        mockMvc.perform(get("/api/v1/marketplace/cart")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", 888L).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result.items").isEmpty())
            .andExpect(jsonPath("$.result.sellerGroups").isEmpty())
            .andExpect(jsonPath("$.result.itemCount").value(0))
            .andExpect(jsonPath("$.result.subtotal").value(0));
    }

    @Test
    void removeCartItem_ExistingItem_ShouldRemoveFromCart() throws Exception {
        // First, get a published product ID from the marketplace
        MvcResult productsResult = mockMvc.perform(get("/api/v1/marketplace/products")
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andReturn();

        String productsJson = productsResult.getResponse().getContentAsString();
        JsonNode productsNode = objectMapper.readTree(productsJson);
        JsonNode items = productsNode.path("result").path("items");

        // Skip test if no products available
        Assumptions.assumeTrue(!items.isEmpty(), "No products available for testing");

        Long productId = items.get(0).path("id").asLong();

        // Add an item to the cart first
        String addItemRequest = String.format("""
            {
                "productId": %d,
                "quantity": 2.0
            }
            """, productId);

        mockMvc.perform(post("/api/v1/marketplace/cart/items")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addItemRequest))
            .andExpect(status().isOk());

        // Now remove the item
        mockMvc.perform(delete("/api/v1/marketplace/cart/items/" + productId)
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void clearCart_WithItems_ShouldRemoveAllItems() throws Exception {
        // First, get a published product ID from the marketplace
        MvcResult productsResult = mockMvc.perform(get("/api/v1/marketplace/products")
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andReturn();

        String productsJson = productsResult.getResponse().getContentAsString();
        JsonNode productsNode = objectMapper.readTree(productsJson);
        JsonNode items = productsNode.path("result").path("items");

        // Skip test if no products available
        Assumptions.assumeTrue(!items.isEmpty(), "No products available for testing");

        Long productId = items.get(0).path("id").asLong();

        // Add an item to the cart
        String addItemRequest = String.format("""
            {
                "productId": %d,
                "quantity": 1.0
            }
            """, productId);

        mockMvc.perform(post("/api/v1/marketplace/cart/items")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addItemRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items").isNotEmpty());

        // Then clear the cart
        mockMvc.perform(delete("/api/v1/marketplace/cart")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result.items").isEmpty())
            .andExpect(jsonPath("$.result.itemCount").value(0))
            .andExpect(jsonPath("$.result.subtotal").value(0));
    }

    @Test
    void clearCart_WithNoCart_ShouldReturnEmptyCart() throws Exception {
        // Use a user ID that has never created a cart
        mockMvc.perform(delete("/api/v1/marketplace/cart")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", 999L).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result.userId").value(999))
            .andExpect(jsonPath("$.result.items").isEmpty())
            .andExpect(jsonPath("$.result.itemCount").value(0))
            .andExpect(jsonPath("$.result.subtotal").value(0));
    }

    @Test
    void updateCartItemQuantity_UsingPatch_ShouldSucceed() throws Exception {
        // First, get a published product ID from the marketplace
        MvcResult productsResult = mockMvc.perform(get("/api/v1/marketplace/products")
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andReturn();

        String productsJson = productsResult.getResponse().getContentAsString();
        JsonNode productsNode = objectMapper.readTree(productsJson);
        JsonNode items = productsNode.path("result").path("items");

        // Skip test if no products available
        Assumptions.assumeTrue(!items.isEmpty(), "No products available for testing");

        Long productId = items.get(0).path("id").asLong();

        // Add an item to the cart first
        String addItemRequest = String.format("""
            {
                "productId": %d,
                "quantity": 2.0
            }
            """, productId);

        mockMvc.perform(post("/api/v1/marketplace/cart/items")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addItemRequest))
            .andExpect(status().isOk());

        // Now update the quantity using PATCH
        String updateRequest = """
            {
                "quantity": 5.0
            }
            """;

        mockMvc.perform(patch("/api/v1/marketplace/cart/items/" + productId)
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void addCartItem_WithActiveProduct_ShouldSucceed() throws Exception {
        // First, get an active product ID from the marketplace
        MvcResult productsResult = mockMvc.perform(get("/api/v1/marketplace/products")
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andReturn();

        String productsJson = productsResult.getResponse().getContentAsString();
        JsonNode productsNode = objectMapper.readTree(productsJson);
        JsonNode items = productsNode.path("result").path("items");

        // Skip test if no products available
        Assumptions.assumeTrue(!items.isEmpty(), "No products available for testing");

        Long productId = items.get(0).path("id").asLong();

        String requestBody = String.format("""
            {
                "productId": %d,
                "quantity": 2.0
            }
            """, productId);

        mockMvc.perform(post("/api/v1/marketplace/cart/items")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void addCartItem_CreatesPersistedCartItemWithGeneratedId() throws Exception {
        User buyer = createUser("buyer");
        MarketplaceProduct product = createPublishedProduct();

        String requestBody = String.format("""
            {
                "productId": %d,
                "quantity": 2.0
            }
            """, product.getId());

        mockMvc.perform(post("/api/v1/marketplace/cart/items")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", buyer.getId()).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"));

        MarketplaceCart cart = marketplaceCartRepository.findByUser_Id(buyer.getId()).orElseThrow();
        MarketplaceCartItem cartItem = marketplaceCartItemRepository
                .findByCart_IdAndProduct_Id(cart.getId(), product.getId())
                .orElseThrow();

        assertNotNull(cartItem.getId());
        assertEquals(0, cartItem.getQuantity().compareTo(new BigDecimal("2.000")));
    }

    @Test
    void getCart_WithMultipleSellers_ShouldGroupBySeller() throws Exception {
        // First, get multiple products from different sellers
        MvcResult productsResult = mockMvc.perform(get("/api/v1/marketplace/products")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andReturn();

        String productsJson = productsResult.getResponse().getContentAsString();
        JsonNode productsNode = objectMapper.readTree(productsJson);
        JsonNode items = productsNode.path("result").path("items");

        // Skip test if not enough products available
        Assumptions.assumeTrue(items.size() >= 2, "Need at least 2 products for testing seller grouping");

        // Add items from different products (potentially different sellers)
        Long productId1 = items.get(0).path("id").asLong();
        Long productId2 = items.get(1).path("id").asLong();

        String addItemRequest1 = String.format("""
            {
                "productId": %d,
                "quantity": 2.0
            }
            """, productId1);

        mockMvc.perform(post("/api/v1/marketplace/cart/items")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addItemRequest1))
            .andExpect(status().isOk());

        String addItemRequest2 = String.format("""
            {
                "productId": %d,
                "quantity": 3.0
            }
            """, productId2);

        mockMvc.perform(post("/api/v1/marketplace/cart/items")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addItemRequest2))
            .andExpect(status().isOk());

        // Now get the cart and verify seller grouping
        MvcResult cartResult = mockMvc.perform(get("/api/v1/marketplace/cart")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result.sellerGroups").isArray())
            .andExpect(jsonPath("$.result.sellerGroups").isNotEmpty())
            .andReturn();

        // Validate seller group structure
        String cartJson = cartResult.getResponse().getContentAsString();
        JsonNode cartNode = objectMapper.readTree(cartJson);
        JsonNode sellerGroups = cartNode.path("result").path("sellerGroups");

        // Each seller group should have required fields
        for (JsonNode group : sellerGroups) {
            // Verify farmerUserId is present and not null
            assertTrue(group.has("farmerUserId"), "Seller group must have farmerUserId field");
            assertFalse(group.path("farmerUserId").isNull(), "farmerUserId must not be null");

            // Verify farmerName is present
            assertTrue(group.has("farmerName"), "Seller group must have farmerName field");
            assertFalse(group.path("farmerName").asText().isEmpty(), "farmerName must not be empty");

            // Verify items array exists and is not empty
            assertTrue(group.has("items"), "Seller group must have items field");
            assertTrue(group.path("items").isArray(), "items must be an array");
            assertFalse(group.path("items").isEmpty(), "items array must not be empty");

            // Verify subtotal is present and >= 0
            assertTrue(group.has("subtotal"), "Seller group must have subtotal field");
            assertTrue(group.path("subtotal").decimalValue().compareTo(java.math.BigDecimal.ZERO) >= 0,
                    "subtotal must be >= 0");

            // Verify each item in the group has the same farmerUserId
            Long groupFarmerId = group.path("farmerUserId").asLong();
            for (JsonNode item : group.path("items")) {
                assertEquals(groupFarmerId, item.path("farmerUserId").asLong(),
                        "All items in a seller group must have the same farmerUserId");
            }
        }
    }

    @Test
    void addCartItem_MissingProduct_ShouldFail() throws Exception {
        String requestBody = """
            {
                "productId": 999,
                "quantity": 1.0
            }
            """;

        mockMvc.perform(post("/api/v1/marketplace/cart/items")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void addCartItem_MissingProduct_SecondUnknownId_ShouldFail() throws Exception {
        String requestBody = """
            {
                "productId": 998,
                "quantity": 1.0
            }
            """;

        mockMvc.perform(post("/api/v1/marketplace/cart/items")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void addCartItem_MissingProduct_ThirdUnknownId_ShouldFail() throws Exception {
        String requestBody = """
            {
                "productId": 997,
                "quantity": 1.0
            }
            """;

        mockMvc.perform(post("/api/v1/marketplace/cart/items")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void addCartItem_QuantityExceedsStock_ShouldFail() throws Exception {
        // First, get a published product ID from the marketplace
        MvcResult productsResult = mockMvc.perform(get("/api/v1/marketplace/products")
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andReturn();

        String productsJson = productsResult.getResponse().getContentAsString();
        JsonNode productsNode = objectMapper.readTree(productsJson);
        JsonNode items = productsNode.path("result").path("items");

        // Skip test if no products available
        Assumptions.assumeTrue(!items.isEmpty(), "No products available for testing");

        JsonNode product = items.get(0);
        Long productId = product.path("id").asLong();
        double maxQuantity = product.path("quantity").asDouble();

        // Request quantity that exceeds available stock by a significant margin
        double excessiveQuantity = maxQuantity + 1000.0;

        String requestBody = String.format("""
            {
                "productId": %d,
                "quantity": %.1f
            }
            """, productId, excessiveQuantity);

        mockMvc.perform(post("/api/v1/marketplace/cart/items")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void addCartItem_NegativeQuantity_ShouldFail() throws Exception {
        // First, get a published product ID from the marketplace
        MvcResult productsResult = mockMvc.perform(get("/api/v1/marketplace/products")
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andReturn();

        String productsJson = productsResult.getResponse().getContentAsString();
        JsonNode productsNode = objectMapper.readTree(productsJson);
        JsonNode items = productsNode.path("result").path("items");

        // Skip test if no products available
        Assumptions.assumeTrue(!items.isEmpty(), "No products available for testing");

        Long productId = items.get(0).path("id").asLong();

        String requestBody = String.format("""
            {
                "productId": %d,
                "quantity": -1.0
            }
            """, productId);

        mockMvc.perform(post("/api/v1/marketplace/cart/items")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ERR_INVALID_REQUEST"));
    }

    /**
     * Tests that multiple sequential cart item updates work correctly.
     * <p>
     * This test performs two PATCH requests sequentially (not concurrently) to verify
     * that the cart update logic handles multiple updates correctly. The test validates
     * that the final quantity reflects the last update operation.
     * </p>
     * <p>
     * <strong>Note:</strong> This is NOT a true concurrency test. It executes requests
     * sequentially in a single thread. True concurrent testing would require multi-threading
     * (e.g., ExecutorService, CountDownLatch) to create actual race conditions, which is
     * complex with MockMvc and Spring's test transaction management.
     * </p>
     * <p>
     * The pessimistic locking mechanism ({@code @Lock(LockModeType.PESSIMISTIC_WRITE)})
     * in the repository layer is verified indirectly by the fact that sequential updates
     * work correctly without data corruption. For production validation of concurrent
     * behavior, consider load testing or integration tests with actual concurrent requests.
     * </p>
     */
    @Test
    void updateCartItem_MultipleSequentialUpdates_ShouldSucceed() throws Exception {
        // First, get a published product ID from the marketplace
        MvcResult productsResult = mockMvc.perform(get("/api/v1/marketplace/products")
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andReturn();

        String productsJson = productsResult.getResponse().getContentAsString();
        JsonNode productsNode = objectMapper.readTree(productsJson);
        JsonNode items = productsNode.path("result").path("items");

        // Skip test if no products available
        Assumptions.assumeTrue(!items.isEmpty(), "No products available for testing");

        Long productId = items.get(0).path("id").asLong();

        // Add an item to the cart first
        String addItemRequest = String.format("""
            {
                "productId": %d,
                "quantity": 2.0
            }
            """, productId);

        mockMvc.perform(post("/api/v1/marketplace/cart/items")
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addItemRequest))
            .andExpect(status().isOk());

        // Perform first update
        String requestBody1 = """
            {
                "quantity": 5.0
            }
            """;

        mockMvc.perform(patch("/api/v1/marketplace/cart/items/" + productId)
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody1))
            .andExpect(status().isOk());

        // Perform second update
        String requestBody2 = """
            {
                "quantity": 3.0
            }
            """;

        mockMvc.perform(patch("/api/v1/marketplace/cart/items/" + productId)
                .with(jwt().jwt(jwt -> jwt.claim("user_id", TEST_BUYER_USER_ID).claim("role", "BUYER")).authorities(() -> "ROLE_BUYER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items[0].quantity").value(3.0));
    }

    private MarketplaceProduct createPublishedProduct() {
        String suffix = UUID.randomUUID().toString();
        User farmer = createUser("farmer");
        Province province = provinceRepository.findById(24)
                .orElseGet(() -> provinceRepository.save(Province.builder()
                        .id(24)
                        .name("Ha Noi")
                        .slug("ha-noi")
                        .type("thanh-pho")
                        .nameWithType("Thanh pho Ha Noi")
                        .build()));
        Ward ward = wardRepository.findById(25112)
                .orElseGet(() -> wardRepository.save(Ward.builder()
                        .id(25112)
                        .name("Phuong Demo")
                        .slug("phuong-demo")
                        .type("phuong")
                        .nameWithType("Phuong Demo, Ha Noi")
                        .province(province)
                        .build()));
        Farm farm = farmRepository.saveAndFlush(Farm.builder()
                .user(farmer)
                .name("Cart Test Farm " + suffix)
                .province(province)
                .ward(ward)
                .area(new BigDecimal("1.00"))
                .build());
        Plot plot = plotRepository.saveAndFlush(Plot.builder()
                .user(farmer)
                .farm(farm)
                .plotName("Cart Test Plot " + suffix)
                .area(new BigDecimal("1.00"))
                .soilType("FERRALSOLS")
                .build());
        Warehouse warehouse = warehouseRepository.saveAndFlush(Warehouse.builder()
                .farm(farm)
                .name("Cart Test Warehouse " + suffix)
                .type("OUTPUT")
                .province(province)
                .ward(ward)
                .build());
        ProductWarehouseLot lot = productWarehouseLotRepository.saveAndFlush(ProductWarehouseLot.builder()
                .lotCode("CART-LOT-" + suffix)
                .productName("Cart Test Rice")
                .farm(farm)
                .plot(plot)
                .warehouse(warehouse)
                .harvestedAt(LocalDate.now())
                .receivedAt(LocalDateTime.now())
                .unit("kg")
                .initialQuantity(new BigDecimal("100.000"))
                .onHandQuantity(new BigDecimal("100.000"))
                .status(ProductWarehouseLotStatus.IN_STOCK)
                .createdBy(farmer)
                .build());

        return marketplaceProductRepository.saveAndFlush(MarketplaceProduct.builder()
                .slug("cart-test-product-" + suffix)
                .name("Cart Test Product")
                .category("rice")
                .price(new BigDecimal("25000.00"))
                .unit("kg")
                .stockQuantity(new BigDecimal("100.000"))
                .farmerUser(farmer)
                .farm(farm)
                .lot(lot)
                .traceable(true)
                .status(MarketplaceProductStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now())
                .build());
    }

    private User createUser(String prefix) {
        String suffix = UUID.randomUUID().toString();
        return userRepository.saveAndFlush(User.builder()
                .username(prefix + "-" + suffix)
                .email(prefix + "-" + suffix + "@test.local")
                .fullName(prefix + " user")
                .phone("0900000000")
                .status(UserStatus.ACTIVE)
                .build());
    }
}
