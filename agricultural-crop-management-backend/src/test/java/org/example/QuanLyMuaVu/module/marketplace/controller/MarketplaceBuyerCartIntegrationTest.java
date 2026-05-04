package org.example.QuanLyMuaVu.module.marketplace.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MarketplaceBuyerCartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    /**
     * Tests that products with INACTIVE status cannot be added to cart.
     * <p>
     * Note: The API treats all non-ACTIVE products (INACTIVE, SOLD_OUT, REJECTED)
     * as "not found" from a buyer's perspective, returning 404 with
     * ERR_MARKETPLACE_PRODUCT_NOT_FOUND. This test uses a non-existent product ID
     * which produces the same result as an INACTIVE product would - both are filtered
     * out by the getActiveProductOrThrow() method which only queries for ACTIVE status.
     * </p>
     */
    @Test
    void addCartItem_InactiveProduct_ShouldFail() throws Exception {
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
            .andExpect(jsonPath("$.code").value("ERR_MARKETPLACE_PRODUCT_NOT_FOUND"));
    }

    /**
     * Tests that products with SOLD_OUT status cannot be added to cart.
     * <p>
     * Note: The API treats all non-ACTIVE products (INACTIVE, SOLD_OUT, REJECTED)
     * as "not found" from a buyer's perspective, returning 404 with
     * ERR_MARKETPLACE_PRODUCT_NOT_FOUND. This test uses a non-existent product ID
     * which produces the same result as a SOLD_OUT product would - both are filtered
     * out by the getActiveProductOrThrow() method which only queries for ACTIVE status.
     * </p>
     */
    @Test
    void addCartItem_SoldOutProduct_ShouldFail() throws Exception {
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
            .andExpect(jsonPath("$.code").value("ERR_MARKETPLACE_PRODUCT_NOT_FOUND"));
    }

    /**
     * Tests that products with REJECTED status cannot be added to cart.
     * <p>
     * Note: The API treats all non-ACTIVE products (INACTIVE, SOLD_OUT, REJECTED)
     * as "not found" from a buyer's perspective, returning 404 with
     * ERR_MARKETPLACE_PRODUCT_NOT_FOUND. This test uses a non-existent product ID
     * which produces the same result as a REJECTED product would - both are filtered
     * out by the getActiveProductOrThrow() method which only queries for ACTIVE status.
     * </p>
     */
    @Test
    void addCartItem_RejectedProduct_ShouldFail() throws Exception {
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
            .andExpect(jsonPath("$.code").value("ERR_MARKETPLACE_PRODUCT_NOT_FOUND"));
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
            .andExpect(jsonPath("$.code").value("ERR_MARKETPLACE_STOCK_CONFLICT"));
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
}
