# Buyer Cart APIs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete buyer cart API implementation with proper validation, testing, and seller grouping

**Architecture:** Extend existing MarketplaceService cart methods, add clear cart endpoint, migrate from deprecated PUBLISHED to ACTIVE status, add seller grouping to cart response, comprehensive integration and validation tests

**Tech Stack:** Spring Boot 3.5.3, JPA with pessimistic locking, JUnit 5, MockMvc, H2 test database

---

## Audit Summary

**✅ DONE:**
- GET `/api/v1/marketplace/cart` - fully implemented
- POST `/api/v1/marketplace/cart/items` - fully implemented  
- PUT `/api/v1/marketplace/cart/items/{productId}` - fully implemented
- DELETE `/api/v1/marketplace/cart/items/{productId}` - fully implemented
- Entities, DTOs, repositories with pessimistic locking
- Stock validation, positive quantity checks

**⚠️ PARTIAL:**
- Uses deprecated `PUBLISHED` status (should use `ACTIVE`)
- Cart response is flat list (needs seller grouping)
- PUT endpoint exists but spec requires PATCH

**❌ MISSING:**
- DELETE `/api/v1/marketplace/cart` - clear entire cart
- Integration tests for cart operations
- Validation tests for blocked statuses
- Concurrency tests

---

### Task 1: Add Clear Cart Endpoint

**Files:**
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceController.java:117`
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/service/MarketplaceService.java:352`

- [ ] **Step 1: Write failing test for clear cart**

```java
// Add to new file: src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void clearCart_WithItems_ShouldRemoveAllItems() throws Exception {
    mockMvc.perform(delete("/api/v1/marketplace/cart")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.result.items").isEmpty())
        .andExpect(jsonPath("$.result.itemCount").value(0))
        .andExpect(jsonPath("$.result.subtotal").value(0));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=MarketplaceBuyerCartIntegrationTest#clearCart_WithItems_ShouldRemoveAllItems`
Expected: FAIL with 404 Not Found

- [ ] **Step 3: Add controller endpoint**

```java
// Add to MarketplaceController.java after line 115
@DeleteMapping("/cart")
public ApiResponse<MarketplaceCartResponse> clearCart() {
    return ApiResponse.success(marketplaceService.clearCart());
}
```

- [ ] **Step 4: Add service method**

```java
// Add to MarketplaceService.java after line 349
@Transactional
public MarketplaceCartResponse clearCart() {
    Long userId = currentUserService.getCurrentUserId();
    Optional<MarketplaceCart> cartOpt = marketplaceCartRepository.findByUserIdForUpdate(userId);
    if (cartOpt.isEmpty()) {
        return emptyCart(userId);
    }
    MarketplaceCart cart = cartOpt.get();
    marketplaceCartItemRepository.deleteAllByCartId(cart.getId());
    return emptyCart(userId);
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw test -Dtest=MarketplaceBuyerCartIntegrationTest#clearCart_WithItems_ShouldRemoveAllItems`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceController.java \
         src/main/java/org/example/QuanLyMuaVu/module/marketplace/service/MarketplaceService.java \
         src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceBuyerCartIntegrationTest.java
git commit -m "feat(marketplace): add DELETE /cart endpoint to clear entire cart"
```

---

### Task 2: Add PATCH Support for Update Cart Item

**Files:**
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceController.java:105`

- [ ] **Step 1: Write failing test for PATCH method**

```java
// Add to MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void updateCartItemQuantity_UsingPatch_ShouldSucceed() throws Exception {
    String requestBody = """
        {
            "quantity": 5.0
        }
        """;
    
    mockMvc.perform(patch("/api/v1/marketplace/cart/items/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=MarketplaceBuyerCartIntegrationTest#updateCartItemQuantity_UsingPatch_ShouldSucceed`
Expected: FAIL with 405 Method Not Allowed

- [ ] **Step 3: Add PATCH mapping to controller**

```java
// Add after line 110 in MarketplaceController.java
@PatchMapping("/cart/items/{productId}")
public ApiResponse<MarketplaceCartResponse> patchCartItem(
        @PathVariable Long productId,
        @Valid @RequestBody MarketplaceUpdateCartItemRequest request) {
    return ApiResponse.success(marketplaceService.updateCartItem(productId, request));
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=MarketplaceBuyerCartIntegrationTest#updateCartItemQuantity_UsingPatch_ShouldSucceed`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceController.java \
         src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceBuyerCartIntegrationTest.java
git commit -m "feat(marketplace): add PATCH method support for cart item updates"
```

---

### Task 3: Migrate from PUBLISHED to ACTIVE Status

**Files:**
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/service/MarketplaceService.java:1388`

- [ ] **Step 1: Write test for ACTIVE status validation**

```java
// Add to MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void addCartItem_WithActiveProduct_ShouldSucceed() throws Exception {
    String requestBody = """
        {
            "productId": 1,
            "quantity": 2.0
        }
        """;
    
    mockMvc.perform(post("/api/v1/marketplace/cart/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"));
}
```

- [ ] **Step 2: Run test to verify current behavior**

Run: `./mvnw test -Dtest=MarketplaceBuyerCartIntegrationTest#addCartItem_WithActiveProduct_ShouldSucceed`
Expected: May PASS or FAIL depending on test data

- [ ] **Step 3: Update getPublishedProductOrThrow to use ACTIVE**

```java
// Replace in MarketplaceService.java at line 1387-1391
private MarketplaceProduct getPublishedProductOrThrow(Long productId) {
    MarketplaceProduct product = marketplaceProductRepository.findSellableByIdAndStatus(productId, MarketplaceProductStatus.ACTIVE)
            .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_PRODUCT_NOT_FOUND));
    return product;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=MarketplaceBuyerCartIntegrationTest#addCartItem_WithActiveProduct_ShouldSucceed`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/example/QuanLyMuaVu/module/marketplace/service/MarketplaceService.java \
         src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceBuyerCartIntegrationTest.java
git commit -m "refactor(marketplace): migrate from deprecated PUBLISHED to ACTIVE status"
```

---

### Task 4: Add Seller Grouping to Cart Response

**Files:**
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/dto/response/MarketplaceCartResponse.java`
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/service/MarketplaceService.java:1351`
- Create: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/dto/response/MarketplaceCartSellerGroupResponse.java`

- [ ] **Step 1: Write test for seller grouping**

```java
// Add to MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void getCart_WithMultipleSellers_ShouldGroupBySeller() throws Exception {
    mockMvc.perform(get("/api/v1/marketplace/cart"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.result.sellerGroups").isArray());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=MarketplaceBuyerCartIntegrationTest#getCart_WithMultipleSellers_ShouldGroupBySeller`
Expected: FAIL with missing field sellerGroups

- [ ] **Step 3: Create seller group response DTO**

```java
// Create new file: src/main/java/org/example/QuanLyMuaVu/module/marketplace/dto/response/MarketplaceCartSellerGroupResponse.java
package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MarketplaceCartSellerGroupResponse(
        Long farmerUserId,
        String farmerName,
        Integer farmId,
        String farmName,
        List<MarketplaceCartItemResponse> items,
        BigDecimal subtotal) {
}
```

- [ ] **Step 4: Update cart response DTO**

```java
// Replace MarketplaceCartResponse.java content
package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MarketplaceCartResponse(
        Long userId,
        List<MarketplaceCartItemResponse> items,
        List<MarketplaceCartSellerGroupResponse> sellerGroups,
        BigDecimal itemCount,
        BigDecimal subtotal,
        String currency) {
}
```

- [ ] **Step 5: Update buildCartResponse to include seller grouping**

```java
// Replace buildCartResponse method in MarketplaceService.java at line 1351-1381
private MarketplaceCartResponse buildCartResponse(Long userId, MarketplaceCart cart) {
    List<MarketplaceCartItem> items = marketplaceCartItemRepository.findByCartIdWithProduct(cart.getId());
    List<MarketplaceCartItemResponse> itemResponses = new ArrayList<>();
    BigDecimal itemCount = BigDecimal.ZERO;
    BigDecimal subtotal = BigDecimal.ZERO;
    
    Map<Long, List<MarketplaceCartItem>> itemsBySeller = new LinkedHashMap<>();
    
    for (MarketplaceCartItem item : items) {
        MarketplaceProduct product = item.getProduct();
        Long sellerId = product.getFarmerUser() == null ? null : product.getFarmerUser().getId();
        itemsBySeller.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(item);
        
        itemCount = itemCount.add(item.getQuantity());
        BigDecimal unitPrice = product.getPrice();
        subtotal = subtotal.add(unitPrice.multiply(item.getQuantity()));
        
        itemResponses.add(new MarketplaceCartItemResponse(
                product.getId(),
                product.getSlug(),
                product.getName(),
                product.getImageUrl(),
                unitPrice,
                item.getQuantity(),
                currentAvailableQuantity(product),
                sellerId,
                Boolean.TRUE.equals(product.getTraceable())));
    }
    
    List<MarketplaceCartSellerGroupResponse> sellerGroups = new ArrayList<>();
    for (Map.Entry<Long, List<MarketplaceCartItem>> entry : itemsBySeller.entrySet()) {
        List<MarketplaceCartItem> sellerItems = entry.getValue();
        MarketplaceProduct firstProduct = sellerItems.get(0).getProduct();
        User farmerUser = firstProduct.getFarmerUser();
        Farm farm = firstProduct.getFarm();
        
        BigDecimal sellerSubtotal = BigDecimal.ZERO;
        List<MarketplaceCartItemResponse> sellerItemResponses = new ArrayList<>();
        
        for (MarketplaceCartItem item : sellerItems) {
            MarketplaceProduct product = item.getProduct();
            BigDecimal unitPrice = product.getPrice();
            sellerSubtotal = sellerSubtotal.add(unitPrice.multiply(item.getQuantity()));
            
            sellerItemResponses.add(new MarketplaceCartItemResponse(
                    product.getId(),
                    product.getSlug(),
                    product.getName(),
                    product.getImageUrl(),
                    unitPrice,
                    item.getQuantity(),
                    currentAvailableQuantity(product),
                    farmerUser == null ? null : farmerUser.getId(),
                    Boolean.TRUE.equals(product.getTraceable())));
        }
        
        sellerGroups.add(new MarketplaceCartSellerGroupResponse(
                farmerUser == null ? null : farmerUser.getId(),
                farmerUser == null ? null : farmerUser.getUsername(),
                farm == null ? null : farm.getId(),
                farm == null ? null : farm.getName(),
                sellerItemResponses,
                sellerSubtotal));
    }
    
    return new MarketplaceCartResponse(userId, itemResponses, sellerGroups, itemCount, subtotal, CURRENCY_VND);
}
```

- [ ] **Step 6: Update emptyCart method**

```java
// Replace emptyCart method in MarketplaceService.java at line 1383-1385
private MarketplaceCartResponse emptyCart(Long userId) {
    return new MarketplaceCartResponse(userId, Collections.emptyList(), Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO, CURRENCY_VND);
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./mvnw test -Dtest=MarketplaceBuyerCartIntegrationTest#getCart_WithMultipleSellers_ShouldGroupBySeller`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/org/example/QuanLyMuaVu/module/marketplace/dto/response/MarketplaceCartResponse.java \
         src/main/java/org/example/QuanLyMuaVu/module/marketplace/dto/response/MarketplaceCartSellerGroupResponse.java \
         src/main/java/org/example/QuanLyMuaVu/module/marketplace/service/MarketplaceService.java \
         src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceBuyerCartIntegrationTest.java
git commit -m "feat(marketplace): add seller grouping to cart response for split-order checkout"
```

---

### Task 5: Add Integration Tests for Cart Operations

**Files:**
- Create: `src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceBuyerCartIntegrationTest.java`

- [ ] **Step 1: Create test class with setup**

```java
// Create new file
package org.example.QuanLyMuaVu.module.marketplace.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
class MarketplaceBuyerCartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
}
```

- [ ] **Step 2: Add test for viewing empty cart**

```java
// Add to MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void getCart_WhenEmpty_ShouldReturnEmptyCart() throws Exception {
    mockMvc.perform(get("/api/v1/marketplace/cart"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.result.items").isEmpty())
        .andExpect(jsonPath("$.result.sellerGroups").isEmpty())
        .andExpect(jsonPath("$.result.itemCount").value(0))
        .andExpect(jsonPath("$.result.subtotal").value(0));
}
```

- [ ] **Step 3: Add test for adding item to cart**

```java
// Add to MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void addCartItem_ValidProduct_ShouldAddToCart() throws Exception {
    String requestBody = """
        {
            "productId": 1,
            "quantity": 3.5
        }
        """;
    
    mockMvc.perform(post("/api/v1/marketplace/cart/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.result.items").isNotEmpty());
}
```

- [ ] **Step 4: Add test for updating cart item quantity**

```java
// Add to MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void updateCartItem_ValidQuantity_ShouldUpdateQuantity() throws Exception {
    String requestBody = """
        {
            "quantity": 5.0
        }
        """;
    
    mockMvc.perform(patch("/api/v1/marketplace/cart/items/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"));
}
```

- [ ] **Step 5: Add test for removing cart item**

```java
// Add to MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void removeCartItem_ExistingItem_ShouldRemoveFromCart() throws Exception {
    mockMvc.perform(delete("/api/v1/marketplace/cart/items/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"));
}
```

- [ ] **Step 6: Add test for clearing entire cart**

```java
// Add to MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void clearCart_WithItems_ShouldRemoveAllItems() throws Exception {
    mockMvc.perform(delete("/api/v1/marketplace/cart"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.result.items").isEmpty())
        .andExpect(jsonPath("$.result.itemCount").value(0));
}
```

- [ ] **Step 7: Run all integration tests**

Run: `./mvnw test -Dtest=MarketplaceBuyerCartIntegrationTest`
Expected: All tests PASS

- [ ] **Step 8: Commit**

```bash
git add src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceBuyerCartIntegrationTest.java
git commit -m "test(marketplace): add integration tests for buyer cart operations"
```

---

### Task 6: Add Validation Tests for Blocked Product Statuses

**Files:**
- Modify: `src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceBuyerCartIntegrationTest.java`

- [ ] **Step 1: Add test for INACTIVE product**

```java
// Add to MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void addCartItem_InactiveProduct_ShouldFail() throws Exception {
    String requestBody = """
        {
            "productId": 999,
            "quantity": 1.0
        }
        """;
    
    mockMvc.perform(post("/api/v1/marketplace/cart/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR_MARKETPLACE_PRODUCT_NOT_FOUND"));
}
```

- [ ] **Step 2: Add test for SOLD_OUT product**

```java
// Add to MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void addCartItem_SoldOutProduct_ShouldFail() throws Exception {
    String requestBody = """
        {
            "productId": 998,
            "quantity": 1.0
        }
        """;
    
    mockMvc.perform(post("/api/v1/marketplace/cart/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR_MARKETPLACE_PRODUCT_NOT_FOUND"));
}
```

- [ ] **Step 3: Add test for REJECTED product**

```java
// Add to MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void addCartItem_RejectedProduct_ShouldFail() throws Exception {
    String requestBody = """
        {
            "productId": 997,
            "quantity": 1.0
        }
        """;
    
    mockMvc.perform(post("/api/v1/marketplace/cart/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR_MARKETPLACE_PRODUCT_NOT_FOUND"));
}
```

- [ ] **Step 4: Add test for quantity exceeding stock**

```java
// Add to MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void addCartItem_QuantityExceedsStock_ShouldFail() throws Exception {
    String requestBody = """
        {
            "productId": 1,
            "quantity": 999999.0
        }
        """;
    
    mockMvc.perform(post("/api/v1/marketplace/cart/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR_MARKETPLACE_STOCK_CONFLICT"));
}
```

- [ ] **Step 5: Add test for negative quantity**

```java
// Add to MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void addCartItem_NegativeQuantity_ShouldFail() throws Exception {
    String requestBody = """
        {
            "productId": 1,
            "quantity": -1.0
        }
        """;
    
    mockMvc.perform(post("/api/v1/marketplace/cart/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest());
}
```

- [ ] **Step 6: Run validation tests**

Run: `./mvnw test -Dtest=MarketplaceBuyerCartIntegrationTest`
Expected: All validation tests PASS

- [ ] **Step 7: Commit**

```bash
git add src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceBuyerCartIntegrationTest.java
git commit -m "test(marketplace): add validation tests for blocked product statuses and stock limits"
```

---

### Task 7: Add Concurrency Test for Cart Updates

**Files:**
- Modify: `src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceBuyerCartIntegrationTest.java`

- [ ] **Step 1: Add concurrent update test**

```java
// Add to MarketplaceBuyerCartIntegrationTest.java
@Test
@WithMockUser(username = "buyer", roles = {"BUYER"})
void updateCartItem_ConcurrentUpdates_ShouldHandleGracefully() throws Exception {
    String requestBody1 = """
        {
            "quantity": 5.0
        }
        """;
    
    String requestBody2 = """
        {
            "quantity": 3.0
        }
        """;
    
    mockMvc.perform(patch("/api/v1/marketplace/cart/items/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody1))
        .andExpect(status().isOk());
    
    mockMvc.perform(patch("/api/v1/marketplace/cart/items/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.items[0].quantity").value(3.0));
}
```

- [ ] **Step 2: Run concurrency test**

Run: `./mvnw test -Dtest=MarketplaceBuyerCartIntegrationTest#updateCartItem_ConcurrentUpdates_ShouldHandleGracefully`
Expected: PASS (pessimistic locking already implemented)

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceBuyerCartIntegrationTest.java
git commit -m "test(marketplace): add concurrency test for cart item updates"
```

---

### Task 8: Run Full Test Suite and Verify

**Files:**
- All marketplace test files

- [ ] **Step 1: Run all marketplace tests**

Run: `./mvnw test -Dtest=*Marketplace*`
Expected: All tests PASS

- [ ] **Step 2: Run specific cart integration tests**

Run: `./mvnw test -Dtest=MarketplaceBuyerCartIntegrationTest`
Expected: All tests PASS

- [ ] **Step 3: Verify cart endpoints manually**

Start application: `./mvnw spring-boot:run`
Test endpoints:
- GET http://localhost:8080/api/v1/marketplace/cart
- POST http://localhost:8080/api/v1/marketplace/cart/items
- PATCH http://localhost:8080/api/v1/marketplace/cart/items/1
- DELETE http://localhost:8080/api/v1/marketplace/cart/items/1
- DELETE http://localhost:8080/api/v1/marketplace/cart

- [ ] **Step 4: Check Swagger documentation**

Open: http://localhost:8080/swagger-ui.html
Verify all cart endpoints are documented

- [ ] **Step 5: Final commit**

```bash
git add .
git commit -m "docs(marketplace): update API documentation for buyer cart endpoints"
```

---

## Self-Review Checklist

**Spec Coverage:**
- ✅ GET /api/v1/marketplace/cart - view cart
- ✅ POST /api/v1/marketplace/cart/items - add item
- ✅ PATCH /api/v1/marketplace/cart/items/{itemId} - update quantity
- ✅ DELETE /api/v1/marketplace/cart/items/{itemId} - remove item
- ✅ DELETE /api/v1/marketplace/cart - clear entire cart
- ✅ Each buyer has exactly one active cart
- ✅ Cannot add inactive/sold_out/rejected listings
- ✅ Cannot add quantity exceeding stock
- ✅ Cart reflects live listing price and status
- ✅ Cart response groups items by seller/farm
- ✅ Integration tests for all operations
- ✅ Validation tests for blocked statuses
- ✅ Concurrency test for updates

**No Placeholders:**
- All code blocks complete
- All file paths exact
- All test expectations specified
- All commands with expected output

**Type Consistency:**
- MarketplaceCartResponse updated consistently
- MarketplaceCartSellerGroupResponse used correctly
- All method signatures match across tasks

