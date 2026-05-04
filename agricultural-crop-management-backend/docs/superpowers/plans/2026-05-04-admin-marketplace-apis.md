# Admin Marketplace APIs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the real Admin Marketplace section 1.8 APIs for listing moderation, payment verification, and marketplace audit history with RBAC and integration coverage.

**Architecture:** Keep the implementation inside the existing marketplace module and reuse `MarketplaceService`, `MarketplaceProductRepository`, `MarketplaceOrderRepository`, `AuditLogService`, `NotificationService`, `ApiResponse`, and `PageResponse`. Add thin admin controllers at the requested `/api/v1/admin/marketplace/**` paths, service methods for business rules, repository search queries for filters, and a small DTO set for request/response shape. Avoid replacing existing legacy `/api/v1/marketplace/admin/**` endpoints; keep them working while adding the required real admin API surface.

**Tech Stack:** Java 23, Spring Boot 3.5.3, Spring MVC, Spring Security, Spring Data JPA, Bean Validation, MySQL/Flyway, H2 tests, JUnit 5, MockMvc, Mockito.

---

## Existing Implementation Audit

### Endpoints

- ❌ `GET /api/v1/admin/marketplace/listings` — missing. Existing partial equivalent is `GET /api/v1/marketplace/admin/products` in `src/main/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminController.java:39`, but it is the wrong path and only supports `q`, `status`, `page`, `size`.
- ❌ `PATCH /api/v1/admin/marketplace/listings/{id}/moderation` — missing. Existing partial equivalent is `PATCH /api/v1/marketplace/admin/products/{productId}/status` in `MarketplaceAdminController.java:48`, but it is the wrong path and does not accept/requires rejection reason.
- ⚠️ `GET /api/v1/admin/marketplace/payment-proofs` — partial. Exists in `MarketplaceAdminPaymentProofController.java:36`, but only supports pagination and always lists `SUBMITTED`; it lacks status, seller/farm, date range, and keyword filters.
- ✅ `PATCH /api/v1/admin/marketplace/payment-proofs/{id}/verify` — exists in `MarketplaceAdminPaymentProofController.java:43` and `MarketplaceService.verifyAdminPaymentProof()`.
- ✅ `PATCH /api/v1/admin/marketplace/payment-proofs/{id}/reject` — exists in `MarketplaceAdminPaymentProofController.java:48` and `MarketplaceService.rejectAdminPaymentProof()`.
- ❌ `GET /api/v1/admin/marketplace/audit-logs` — missing. Existing order-scoped equivalent is `GET /api/v1/marketplace/admin/orders/{orderId}/audit-logs` in `MarketplaceAdminController.java:82`; global admin audit logs exist at `/api/v1/admin/audit-logs` but not marketplace-specific.

### Business Rules

- ❌ Reject listing requires mandatory non-empty reason — missing; `MarketplaceUpdateProductStatusRequest` only contains `status`.
- ⚠️ Reject payment proof requires mandatory non-empty reason — DTO has `@NotBlank` in `MarketplaceRejectPaymentProofRequest.java:5`; service also normalizes and throws `BAD_REQUEST` in `MarketplaceService.java:1232`. Needs MockMvc integration coverage on real endpoint.
- ✅ Listing approve moves status to `ACTIVE` — supported by `updateAdminProductStatus()` in `MarketplaceService.java:1635`.
- ❌ Listing reject moves status to `REJECTED` and reason stored — status can move to `REJECTED`, but `MarketplaceProduct` has no moderation reason field and no migration column.
- ✅ Proof verify auto-advances order to `PAYMENT_VERIFIED` — implemented in `MarketplaceService.java:1192`.
- ✅ Proof reject keeps order status and buyer notified with reason — service does not mutate order status in `MarketplaceService.java:1218`; notification includes reason at `MarketplaceService.java:1246`.
- ⚠️ All approve/reject/verify actions recorded in audit log — product status and payment proof actions log via `auditProductStatusChange()` / `auditOrderOperation()`, but listing rejection reason is not stored as a product field and new endpoints need audit tests.
- ⚠️ Filters on all list endpoints: status, seller/farm, date range, keyword — product admin list has only status + keyword; payment proof list has none except implicit submitted status; audit list endpoint is missing.
- ✅ Standard pagination on list endpoints — product and payment proof lists already use `PageResponse`, but new audit list must also paginate.

### Tests

- ⚠️ RBAC tests: Farmer and Buyer cannot access any `/admin/**` endpoints — tests exist for legacy `/api/v1/marketplace/admin/products` in `MarketplaceSecurityTest.java:113`; required `/api/v1/admin/marketplace/**` endpoints need coverage.
- ⚠️ Integration tests: approve listing, reject listing with reason, verify proof, reject proof with reason — legacy listing tests exist in `MarketplaceAdminControllerIntegrationTest.java:129`; service-level payment proof tests exist in `PaymentProofIntegrationTest.java:139`; required new endpoints need MockMvc integration tests.
- ❌ Audit log test: verify log entry created for each moderation action — missing for the required admin marketplace actions.

---

## File Structure

### Create

- `src/main/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminListingController.java` — new controller for `/api/v1/admin/marketplace/listings`.
- `src/main/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminAuditLogController.java` — new controller for `/api/v1/admin/marketplace/audit-logs`.
- `src/main/java/org/example/QuanLyMuaVu/module/marketplace/dto/request/MarketplaceListingModerationRequest.java` — approve/reject request with status and reason validation.
- `src/main/java/org/example/QuanLyMuaVu/module/marketplace/dto/response/MarketplaceAuditLogPageItemResponse.java` — marketplace audit log list item.
- `src/main/resources/db/migration/V24__admin_marketplace_moderation_reason.sql` — add product moderation reason/date/admin columns and indexes.
- `src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminListingApiIntegrationTest.java` — MockMvc tests for new listing endpoints.
- `src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminPaymentProofApiIntegrationTest.java` — MockMvc tests for new payment proof endpoints.
- `src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminAuditLogApiIntegrationTest.java` — MockMvc tests for marketplace audit list and action-created audit entries.
- `src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminRbacIntegrationTest.java` — RBAC tests for farmer/buyer forbidden on required `/api/v1/admin/marketplace/**` endpoints.

### Modify

- `src/main/java/org/example/QuanLyMuaVu/module/marketplace/entity/MarketplaceProduct.java` — add `moderationReason`, `moderatedAt`, `moderatedByUserId` fields.
- `src/main/java/org/example/QuanLyMuaVu/module/marketplace/dto/response/MarketplaceProductSummaryResponse.java` — expose moderation reason fields if summary already carries admin-list data.
- `src/main/java/org/example/QuanLyMuaVu/module/marketplace/dto/response/MarketplaceProductDetailResponse.java` — expose moderation reason fields for moderation action response.
- `src/main/java/org/example/QuanLyMuaVu/module/marketplace/repository/MarketplaceProductRepository.java` — extend admin product search with seller/farm/date filters.
- `src/main/java/org/example/QuanLyMuaVu/module/marketplace/repository/MarketplaceOrderRepository.java` — add payment proof admin search with status/seller/farm/date/keyword filters.
- `src/main/java/org/example/QuanLyMuaVu/module/marketplace/service/MarketplaceService.java` — add new listing moderation methods, filtered proof list method, paginated marketplace audit log method, response mapping fields, and audit reason behavior.
- `src/main/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminPaymentProofController.java` — add filter request params and pass them to service.
- `src/main/java/org/example/QuanLyMuaVu/module/admin/service/AuditLogService.java` and repository if needed — add paginated marketplace entity filtering if no existing method supports it.

---

## Task 1: Add Listing Moderation Reason Persistence

**Files:**
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/entity/MarketplaceProduct.java`
- Create: `src/main/resources/db/migration/V24__admin_marketplace_moderation_reason.sql`
- Test: `src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminListingApiIntegrationTest.java`

- [ ] **Step 1: Write the failing listing rejection persistence test**

Create `src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminListingApiIntegrationTest.java` with this first test scaffold. Reuse existing setup style from `MarketplaceAdminControllerIntegrationTest`.

```java
package org.example.QuanLyMuaVu.module.marketplace.controller;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import org.example.QuanLyMuaVu.Enums.ProductWarehouseLotStatus;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.repository.FarmRepository;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.identity.repository.UserRepository;
import org.example.QuanLyMuaVu.module.inventory.entity.ProductWarehouseLot;
import org.example.QuanLyMuaVu.module.inventory.repository.ProductWarehouseLotRepository;
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
class MarketplaceAdminListingApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MarketplaceProductRepository marketplaceProductRepository;
    @Autowired UserRepository userRepository;
    @Autowired FarmRepository farmRepository;
    @Autowired SeasonRepository seasonRepository;
    @Autowired ProductWarehouseLotRepository productWarehouseLotRepository;

    private Long pendingProductId;

    @BeforeEach
    void setUp() {
        User farmer = userRepository.findByUsername("farmer").orElseThrow();
        Farm farm = farmRepository.findAll().stream()
                .filter(item -> item.getUser() != null && item.getUser().getId().equals(farmer.getId()))
                .findFirst()
                .orElse(null);
        assumeTrue(farm != null, "Test requires seeded farmer farm");

        Season season = seasonRepository.findAll().stream()
                .filter(item -> item.getPlot() != null
                        && item.getPlot().getFarm() != null
                        && item.getPlot().getFarm().getId().equals(farm.getId()))
                .findFirst()
                .orElse(null);

        ProductWarehouseLot lot = productWarehouseLotRepository.findAll().stream()
                .filter(item -> item.getFarm() != null
                        && item.getFarm().getId().equals(farm.getId())
                        && item.getStatus() == ProductWarehouseLotStatus.IN_STOCK
                        && item.getOnHandQuantity() != null
                        && item.getOnHandQuantity().compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElseGet(() -> {
                    ProductWarehouseLot created = new ProductWarehouseLot();
                    created.setLotCode("ADMIN-LISTING-LOT-" + System.nanoTime());
                    created.setProductName("Admin Listing Rice");
                    created.setInitialQuantity(new BigDecimal("100"));
                    created.setOnHandQuantity(new BigDecimal("100"));
                    created.setUnit("kg");
                    created.setStatus(ProductWarehouseLotStatus.IN_STOCK);
                    created.setFarm(farm);
                    created.setSeason(season);
                    return productWarehouseLotRepository.save(created);
                });

        MarketplaceProduct product = new MarketplaceProduct();
        product.setSlug("admin-listing-" + System.nanoTime());
        product.setName("Admin Listing Test Rice");
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
        pendingProductId = marketplaceProductRepository.save(product).getId();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void rejectListing_WithReason_ShouldStoreReason() throws Exception {
        Map<String, Object> request = Map.of(
                "status", "REJECTED",
                "reason", "Images do not match the harvested lot");

        mockMvc.perform(patch("/api/v1/admin/marketplace/listings/{id}/moderation", pendingProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("REJECTED"))
                .andExpect(jsonPath("$.result.moderationReason").value("Images do not match the harvested lot"));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
./mvnw test -Dtest=MarketplaceAdminListingApiIntegrationTest#rejectListing_WithReason_ShouldStoreReason
```

Expected: FAIL because `/api/v1/admin/marketplace/listings/{id}/moderation` does not exist and/or `moderationReason` is not mapped.

- [ ] **Step 3: Add the migration**

Create `src/main/resources/db/migration/V24__admin_marketplace_moderation_reason.sql`:

```sql
ALTER TABLE marketplace_products
    ADD COLUMN moderation_reason VARCHAR(1000) NULL AFTER status,
    ADD COLUMN moderated_at DATETIME NULL AFTER moderation_reason,
    ADD COLUMN moderated_by_user_id BIGINT NULL AFTER moderated_at;

CREATE INDEX idx_marketplace_products_moderated_at ON marketplace_products(moderated_at);
CREATE INDEX idx_marketplace_products_moderated_by ON marketplace_products(moderated_by_user_id);
```

- [ ] **Step 4: Add fields to `MarketplaceProduct`**

In `MarketplaceProduct.java`, after `status`, add:

```java
@Column(name = "moderation_reason", length = 1000)
String moderationReason;

@Column(name = "moderated_at")
LocalDateTime moderatedAt;

@Column(name = "moderated_by_user_id")
Long moderatedByUserId;
```

- [ ] **Step 5: Run the targeted test again**

Run:

```bash
./mvnw test -Dtest=MarketplaceAdminListingApiIntegrationTest#rejectListing_WithReason_ShouldStoreReason
```

Expected: still FAIL because the controller/service/response mapping does not exist yet.

---

## Task 2: Add Listing Moderation DTO, Controller, and Service Method

**Files:**
- Create: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/dto/request/MarketplaceListingModerationRequest.java`
- Create: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminListingController.java`
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/service/MarketplaceService.java`
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/dto/response/MarketplaceProductDetailResponse.java`
- Test: `src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminListingApiIntegrationTest.java`

- [ ] **Step 1: Add approve and missing-reason tests**

Append these tests to `MarketplaceAdminListingApiIntegrationTest`:

```java
@Test
@WithMockUser(username = "admin", roles = "ADMIN")
void approveListing_ShouldMoveToActive() throws Exception {
    Map<String, Object> request = Map.of("status", "ACTIVE");

    mockMvc.perform(patch("/api/v1/admin/marketplace/listings/{id}/moderation", pendingProductId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.status").value("ACTIVE"))
            .andExpect(jsonPath("$.result.publishedAt").exists());
}

@Test
@WithMockUser(username = "admin", roles = "ADMIN")
void rejectListing_WithoutReason_ShouldReturnBadRequest() throws Exception {
    Map<String, Object> request = Map.of("status", "REJECTED", "reason", "   ");

    mockMvc.perform(patch("/api/v1/admin/marketplace/listings/{id}/moderation", pendingProductId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}
```

- [ ] **Step 2: Run tests to verify failures**

Run:

```bash
./mvnw test -Dtest=MarketplaceAdminListingApiIntegrationTest
```

Expected: FAIL because the new endpoint is missing.

- [ ] **Step 3: Create request DTO**

Create `MarketplaceListingModerationRequest.java`:

```java
package org.example.QuanLyMuaVu.module.marketplace.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;

public record MarketplaceListingModerationRequest(
        @NotNull MarketplaceProductStatus status,
        String reason) {
}
```

- [ ] **Step 4: Create controller**

Create `MarketplaceAdminListingController.java`:

```java
package org.example.QuanLyMuaVu.module.marketplace.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceListingModerationRequest;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceProductDetailResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceProductSummaryResponse;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplaceProductStatus;
import org.example.QuanLyMuaVu.module.marketplace.service.MarketplaceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/marketplace/listings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MarketplaceAdminListingController {

    MarketplaceService marketplaceService;

    @GetMapping
    public ApiResponse<PageResponse<MarketplaceProductSummaryResponse>> listListings(
            @RequestParam(value = "status", required = false) MarketplaceProductStatus status,
            @RequestParam(value = "sellerId", required = false) Long sellerId,
            @RequestParam(value = "farmId", required = false) Integer farmId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(marketplaceService.listAdminMarketplaceListings(
                status, sellerId, farmId, fromDate, toDate, keyword, page, size));
    }

    @PatchMapping("/{id}/moderation")
    public ApiResponse<MarketplaceProductDetailResponse> moderateListing(
            @PathVariable Long id,
            @Valid @RequestBody MarketplaceListingModerationRequest request) {
        return ApiResponse.success(marketplaceService.moderateAdminMarketplaceListing(id, request));
    }
}
```

- [ ] **Step 5: Add service method**

In `MarketplaceService.java`, add imports:

```java
import java.time.LocalDate;
import org.example.QuanLyMuaVu.module.marketplace.dto.request.MarketplaceListingModerationRequest;
```

Add this method near existing admin product methods:

```java
@Transactional
public MarketplaceProductDetailResponse moderateAdminMarketplaceListing(
        Long productId,
        MarketplaceListingModerationRequest request) {
    MarketplaceProduct product = marketplaceProductRepository.findById(productId)
            .orElseThrow(() -> new AppException(ErrorCode.MARKETPLACE_PRODUCT_NOT_FOUND));
    MarketplaceProductStatus targetStatus = request.status();
    if (targetStatus != MarketplaceProductStatus.ACTIVE && targetStatus != MarketplaceProductStatus.REJECTED) {
        throw new AppException(ErrorCode.BAD_REQUEST);
    }

    String reason = normalizeNullable(request.reason());
    if (targetStatus == MarketplaceProductStatus.REJECTED && reason == null) {
        throw new AppException(ErrorCode.BAD_REQUEST);
    }
    if (targetStatus == MarketplaceProductStatus.ACTIVE) {
        ProductWarehouseLot lot = product.getLot();
        if (lot == null) {
            throw new AppException(ErrorCode.MARKETPLACE_TRACEABILITY_CHAIN_INVALID);
        }
        ProductWarehouseLot lockedLot = productWarehouseLotRepository.findById(lot.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_WAREHOUSE_LOT_NOT_FOUND));
        ensureLotSellable(lockedLot);
        ensureListingHasStock(product);
        product.setPublishedAt(product.getPublishedAt() == null ? LocalDateTime.now() : product.getPublishedAt());
        product.setModerationReason(null);
    } else {
        product.setPublishedAt(null);
        product.setModerationReason(reason);
    }

    User admin = currentUserService.getCurrentUser();
    product.setStatus(targetStatus);
    product.setModeratedAt(LocalDateTime.now());
    product.setModeratedByUserId(admin.getId());

    MarketplaceProduct saved = marketplaceProductRepository.save(product);
    auditProductStatusChange(saved, targetStatus == MarketplaceProductStatus.ACTIVE
            ? "Admin approved listing"
            : "Admin rejected listing: " + reason);
    if (saved.getFarmerUser() != null) {
        notifyUser(
                saved.getFarmerUser().getId(),
                targetStatus == MarketplaceProductStatus.ACTIVE ? "Listing approved" : "Listing rejected",
                targetStatus == MarketplaceProductStatus.ACTIVE
                        ? "Listing " + saved.getName() + " is now active."
                        : "Listing " + saved.getName() + " was rejected. Reason: " + reason,
                "/farmer/marketplace-products");
    }

    MarketplaceProductReviewRepository.ProductRatingProjection rating = aggregateProductRatings(List.of(saved.getId()))
            .get(saved.getId());
    return toProductDetail(saved, rating);
}
```

- [ ] **Step 6: Expose moderation fields in detail response**

Inspect `MarketplaceProductDetailResponse` constructor and `toProductDetail(...)`. Add fields:

```java
String moderationReason,
LocalDateTime moderatedAt,
Long moderatedByUserId
```

Pass these values from the product in `toProductDetail(...)`:

```java
product.getModerationReason(),
product.getModeratedAt(),
product.getModeratedByUserId()
```

- [ ] **Step 7: Run listing tests**

Run:

```bash
./mvnw test -Dtest=MarketplaceAdminListingApiIntegrationTest
```

Expected: PASS.

---

## Task 3: Add Listing List Filters

**Files:**
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/repository/MarketplaceProductRepository.java`
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/service/MarketplaceService.java`
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/dto/response/MarketplaceProductSummaryResponse.java`
- Test: `src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminListingApiIntegrationTest.java`

- [ ] **Step 1: Add filter test**

Append:

```java
@Test
@WithMockUser(username = "admin", roles = "ADMIN")
void listListings_WithFilters_ShouldReturnPagedResults() throws Exception {
    MarketplaceProduct product = marketplaceProductRepository.findById(pendingProductId).orElseThrow();

    mockMvc.perform(get("/api/v1/admin/marketplace/listings")
                    .param("status", "PENDING_REVIEW")
                    .param("sellerId", product.getFarmerUser().getId().toString())
                    .param("farmId", product.getFarm().getId().toString())
                    .param("keyword", "Admin Listing")
                    .param("page", "0")
                    .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items").isArray())
            .andExpect(jsonPath("$.result.items[0].id").value(pendingProductId));
}
```

Add static import:

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
```

- [ ] **Step 2: Run test to verify failure**

Run:

```bash
./mvnw test -Dtest=MarketplaceAdminListingApiIntegrationTest#listListings_WithFilters_ShouldReturnPagedResults
```

Expected: FAIL until repository/service filter method is implemented.

- [ ] **Step 3: Replace or overload admin product search query**

In `MarketplaceProductRepository.java`, add:

```java
@Query(value = """
        SELECT p FROM MarketplaceProduct p
        JOIN p.lot lot
        LEFT JOIN p.farm f
        LEFT JOIN p.farmerUser fu
        WHERE (:status IS NULL OR p.status = :status)
          AND (:sellerId IS NULL OR fu.id = :sellerId)
          AND (:farmId IS NULL OR f.id = :farmId)
          AND (:fromDateTime IS NULL OR p.createdAt >= :fromDateTime)
          AND (:toDateTime IS NULL OR p.createdAt < :toDateTime)
          AND (:keyword IS NULL
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(f.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(fu.fullName, COALESCE(fu.username, COALESCE(fu.email, '')))) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """, countQuery = """
        SELECT COUNT(p) FROM MarketplaceProduct p
        JOIN p.lot lot
        LEFT JOIN p.farm f
        LEFT JOIN p.farmerUser fu
        WHERE (:status IS NULL OR p.status = :status)
          AND (:sellerId IS NULL OR fu.id = :sellerId)
          AND (:farmId IS NULL OR f.id = :farmId)
          AND (:fromDateTime IS NULL OR p.createdAt >= :fromDateTime)
          AND (:toDateTime IS NULL OR p.createdAt < :toDateTime)
          AND (:keyword IS NULL
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(f.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(fu.fullName, COALESCE(fu.username, COALESCE(fu.email, '')))) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
Page<MarketplaceProduct> searchAdminMarketplaceListings(
        @Param("status") MarketplaceProductStatus status,
        @Param("sellerId") Long sellerId,
        @Param("farmId") Integer farmId,
        @Param("fromDateTime") LocalDateTime fromDateTime,
        @Param("toDateTime") LocalDateTime toDateTime,
        @Param("keyword") String keyword,
        Pageable pageable);
```

Add import:

```java
import java.time.LocalDateTime;
```

- [ ] **Step 4: Add service list method**

In `MarketplaceService.java`, add:

```java
@Transactional(readOnly = true)
public PageResponse<MarketplaceProductSummaryResponse> listAdminMarketplaceListings(
        MarketplaceProductStatus status,
        Long sellerId,
        Integer farmId,
        LocalDate fromDate,
        LocalDate toDate,
        String keyword,
        int page,
        int size) {
    Pageable pageable = PageRequest.of(
            Math.max(0, page),
            Math.max(1, size),
            Sort.by(Sort.Direction.DESC, "updatedAt"));
    LocalDateTime fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
    LocalDateTime toDateTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay();
    Page<MarketplaceProduct> productPage = marketplaceProductRepository.searchAdminMarketplaceListings(
            status,
            sellerId,
            farmId,
            fromDateTime,
            toDateTime,
            normalizeNullable(keyword),
            pageable);
    Map<Long, MarketplaceProductReviewRepository.ProductRatingProjection> ratings = aggregateProductRatings(
            productPage.getContent().stream().map(MarketplaceProduct::getId).toList());
    List<MarketplaceProductSummaryResponse> items = productPage.getContent().stream()
            .map(product -> toProductSummary(product, ratings.get(product.getId())))
            .toList();
    return PageResponse.of(productPage, items);
}
```

- [ ] **Step 5: Run listing tests**

Run:

```bash
./mvnw test -Dtest=MarketplaceAdminListingApiIntegrationTest
```

Expected: PASS.

---

## Task 4: Add Payment Proof List Filters and Endpoint Tests

**Files:**
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminPaymentProofController.java`
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/repository/MarketplaceOrderRepository.java`
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/service/MarketplaceService.java`
- Test: `src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminPaymentProofApiIntegrationTest.java`

- [ ] **Step 1: Create payment proof endpoint tests**

Create `MarketplaceAdminPaymentProofApiIntegrationTest.java` with tests for list filters, verify, reject reason, and missing reject reason. Use seeded or locally created order data patterned after `PaymentProofIntegrationTest`. The key assertions must hit these URLs:

```java
mockMvc.perform(get("/api/v1/admin/marketplace/payment-proofs")
        .param("status", "SUBMITTED")
        .param("sellerId", farmerId.toString())
        .param("farmId", farmId.toString())
        .param("keyword", "MO-")
        .param("page", "0")
        .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.items").isArray());

mockMvc.perform(patch("/api/v1/admin/marketplace/payment-proofs/{id}/verify", orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.verificationStatus").value("VERIFIED"))
        .andExpect(jsonPath("$.result.orderStatus").value("PAYMENT_VERIFIED"));

mockMvc.perform(patch("/api/v1/admin/marketplace/payment-proofs/{id}/reject", orderId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("reason", "Transfer amount mismatch"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.verificationStatus").value("REJECTED"))
        .andExpect(jsonPath("$.result.verificationNote").value("Transfer amount mismatch"));

mockMvc.perform(patch("/api/v1/admin/marketplace/payment-proofs/{id}/reject", orderId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("reason", "   "))))
        .andExpect(status().isBadRequest());
```

- [ ] **Step 2: Run tests to verify list filter failure**

Run:

```bash
./mvnw test -Dtest=MarketplaceAdminPaymentProofApiIntegrationTest
```

Expected: FAIL because list filters are not wired and endpoint tests are new.

- [ ] **Step 3: Add repository search query**

In `MarketplaceOrderRepository.java`, add:

```java
@Query(value = """
        SELECT DISTINCT o FROM MarketplaceOrder o
        LEFT JOIN o.farmerUser fu
        LEFT JOIN o.items oi
        LEFT JOIN oi.product p
        LEFT JOIN p.farm f
        WHERE (:status IS NULL OR o.paymentVerificationStatus = :status)
          AND (:sellerId IS NULL OR fu.id = :sellerId)
          AND (:farmId IS NULL OR f.id = :farmId)
          AND (:fromDateTime IS NULL OR o.paymentProofUploadedAt >= :fromDateTime)
          AND (:toDateTime IS NULL OR o.paymentProofUploadedAt < :toDateTime)
          AND (:keyword IS NULL
               OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(fu.fullName, COALESCE(fu.username, COALESCE(fu.email, '')))) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """, countQuery = """
        SELECT COUNT(DISTINCT o) FROM MarketplaceOrder o
        LEFT JOIN o.farmerUser fu
        LEFT JOIN o.items oi
        LEFT JOIN oi.product p
        LEFT JOIN p.farm f
        WHERE (:status IS NULL OR o.paymentVerificationStatus = :status)
          AND (:sellerId IS NULL OR fu.id = :sellerId)
          AND (:farmId IS NULL OR f.id = :farmId)
          AND (:fromDateTime IS NULL OR o.paymentProofUploadedAt >= :fromDateTime)
          AND (:toDateTime IS NULL OR o.paymentProofUploadedAt < :toDateTime)
          AND (:keyword IS NULL
               OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(fu.fullName, COALESCE(fu.username, COALESCE(fu.email, '')))) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
Page<MarketplaceOrder> searchAdminPaymentProofs(
        @Param("status") MarketplacePaymentVerificationStatus status,
        @Param("sellerId") Long sellerId,
        @Param("farmId") Integer farmId,
        @Param("fromDateTime") LocalDateTime fromDateTime,
        @Param("toDateTime") LocalDateTime toDateTime,
        @Param("keyword") String keyword,
        Pageable pageable);
```

Add import:

```java
import java.time.LocalDateTime;
```

- [ ] **Step 4: Update controller params**

In `MarketplaceAdminPaymentProofController.java`, change `listPaymentProofs` to:

```java
@GetMapping
public ApiResponse<PageResponse<MarketplacePaymentProofResponse>> listPaymentProofs(
        @RequestParam(value = "status", required = false) MarketplacePaymentVerificationStatus status,
        @RequestParam(value = "sellerId", required = false) Long sellerId,
        @RequestParam(value = "farmId", required = false) Integer farmId,
        @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size) {
    return ApiResponse.success(marketplaceService.listAdminPaymentProofs(
            status, sellerId, farmId, fromDate, toDate, keyword, page, size));
}
```

Add imports:

```java
import java.time.LocalDate;
import org.example.QuanLyMuaVu.module.marketplace.model.MarketplacePaymentVerificationStatus;
import org.springframework.format.annotation.DateTimeFormat;
```

- [ ] **Step 5: Update service list method**

Replace existing `listAdminPaymentProofs(int page, int size)` with:

```java
@Transactional(readOnly = true)
public PageResponse<MarketplacePaymentProofResponse> listAdminPaymentProofs(
        MarketplacePaymentVerificationStatus status,
        Long sellerId,
        Integer farmId,
        LocalDate fromDate,
        LocalDate toDate,
        String keyword,
        int page,
        int size) {
    Pageable pageable = PageRequest.of(
            Math.max(0, page),
            Math.max(1, size),
            Sort.by(Sort.Direction.ASC, "paymentProofUploadedAt"));
    LocalDateTime fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
    LocalDateTime toDateTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay();
    MarketplacePaymentVerificationStatus effectiveStatus = status == null
            ? MarketplacePaymentVerificationStatus.SUBMITTED
            : status;
    Page<MarketplaceOrder> orderPage = marketplaceOrderRepository.searchAdminPaymentProofs(
            effectiveStatus,
            sellerId,
            farmId,
            fromDateTime,
            toDateTime,
            normalizeNullable(keyword),
            pageable);
    List<MarketplacePaymentProofResponse> items = orderPage.getContent().stream()
            .map(this::toPaymentProofResponse)
            .toList();
    return PageResponse.of(orderPage, items);
}
```

- [ ] **Step 6: Run payment proof tests**

Run:

```bash
./mvnw test -Dtest=MarketplaceAdminPaymentProofApiIntegrationTest,PaymentProofIntegrationTest
```

Expected: PASS.

---

## Task 5: Add Marketplace Audit Log List Endpoint

**Files:**
- Create: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminAuditLogController.java`
- Create: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/dto/response/MarketplaceAuditLogPageItemResponse.java`
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/marketplace/service/MarketplaceService.java`
- Modify: `src/main/java/org/example/QuanLyMuaVu/module/admin/service/AuditLogService.java`
- Modify: audit log repository if needed by current service API
- Test: `src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminAuditLogApiIntegrationTest.java`

- [ ] **Step 1: Write audit list test**

Create `MarketplaceAdminAuditLogApiIntegrationTest.java` with a test that performs one listing moderation action, then lists marketplace audit logs:

```java
@Test
@WithMockUser(username = "admin", roles = "ADMIN")
void listAuditLogs_AfterListingApproval_ShouldIncludeProductStatusChange() throws Exception {
    Long productId = createPendingProduct();

    mockMvc.perform(patch("/api/v1/admin/marketplace/listings/{id}/moderation", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("status", "ACTIVE"))))
            .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/admin/marketplace/audit-logs")
                    .param("entityType", "MARKETPLACE_PRODUCT")
                    .param("keyword", "PRODUCT_STATUS_CHANGED")
                    .param("page", "0")
                    .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.items").isArray())
            .andExpect(jsonPath("$.result.items[0].entityType").value("MARKETPLACE_PRODUCT"));
}
```

Use the same `createPendingProduct()` helper structure from Task 1.

- [ ] **Step 2: Run audit test to verify failure**

Run:

```bash
./mvnw test -Dtest=MarketplaceAdminAuditLogApiIntegrationTest#listAuditLogs_AfterListingApproval_ShouldIncludeProductStatusChange
```

Expected: FAIL because `/api/v1/admin/marketplace/audit-logs` is missing.

- [ ] **Step 3: Create response DTO**

Create `MarketplaceAuditLogPageItemResponse.java`:

```java
package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.time.LocalDateTime;

public record MarketplaceAuditLogPageItemResponse(
        Long id,
        String entityType,
        Integer entityId,
        String operation,
        String performedBy,
        LocalDateTime performedAt,
        String reason,
        String snapshotDataJson) {
}
```

- [ ] **Step 4: Add audit log service/repository method**

Inspect `AuditLogService` and its repository. Add a paginated query equivalent to:

```java
Page<AuditLog> searchMarketplaceAuditLogs(
        String entityType,
        LocalDateTime fromDateTime,
        LocalDateTime toDateTime,
        String keyword,
        Pageable pageable);
```

The query must restrict marketplace history to these entity types unless `entityType` is more specific:

```java
List.of("MARKETPLACE_PRODUCT", "MARKETPLACE_ORDER")
```

Keyword should match `operation`, `performedBy`, or `reason`.

- [ ] **Step 5: Add marketplace service method**

In `MarketplaceService.java`, add:

```java
@Transactional(readOnly = true)
public PageResponse<MarketplaceAuditLogPageItemResponse> listMarketplaceAuditLogs(
        String entityType,
        LocalDate fromDate,
        LocalDate toDate,
        String keyword,
        int page,
        int size) {
    Pageable pageable = PageRequest.of(
            Math.max(0, page),
            Math.max(1, size),
            Sort.by(Sort.Direction.DESC, "performedAt"));
    LocalDateTime fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
    LocalDateTime toDateTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay();
    Page<AuditLog> auditPage = auditLogService.searchMarketplaceAuditLogs(
            normalizeNullable(entityType),
            fromDateTime,
            toDateTime,
            normalizeNullable(keyword),
            pageable);
    List<MarketplaceAuditLogPageItemResponse> items = auditPage.getContent().stream()
            .map(log -> new MarketplaceAuditLogPageItemResponse(
                    log.getId(),
                    log.getEntityType(),
                    log.getEntityId(),
                    log.getOperation(),
                    log.getPerformedBy(),
                    log.getPerformedAt(),
                    log.getReason(),
                    log.getSnapshotDataJson()))
            .toList();
    return PageResponse.of(auditPage, items);
}
```

- [ ] **Step 6: Create controller**

Create `MarketplaceAdminAuditLogController.java`:

```java
package org.example.QuanLyMuaVu.module.marketplace.controller;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceAuditLogPageItemResponse;
import org.example.QuanLyMuaVu.module.marketplace.service.MarketplaceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/marketplace/audit-logs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MarketplaceAdminAuditLogController {

    MarketplaceService marketplaceService;

    @GetMapping
    public ApiResponse<PageResponse<MarketplaceAuditLogPageItemResponse>> listAuditLogs(
            @RequestParam(value = "entityType", required = false) String entityType,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(marketplaceService.listMarketplaceAuditLogs(
                entityType, fromDate, toDate, keyword, page, size));
    }
}
```

- [ ] **Step 7: Run audit tests**

Run:

```bash
./mvnw test -Dtest=MarketplaceAdminAuditLogApiIntegrationTest
```

Expected: PASS.

---

## Task 6: Add RBAC Tests for Required `/admin/**` Endpoints

**Files:**
- Create: `src/test/java/org/example/QuanLyMuaVu/module/marketplace/controller/MarketplaceAdminRbacIntegrationTest.java`

- [ ] **Step 1: Write RBAC tests**

Create:

```java
package org.example.QuanLyMuaVu.module.marketplace.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MarketplaceAdminRbacIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "farmer", roles = "FARMER")
    void farmerCannotAccessAdminMarketplaceEndpoints() throws Exception {
        assertForbiddenForAllAdminMarketplaceEndpoints();
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void buyerCannotAccessAdminMarketplaceEndpoints() throws Exception {
        assertForbiddenForAllAdminMarketplaceEndpoints();
    }

    private void assertForbiddenForAllAdminMarketplaceEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/listings"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/admin/marketplace/listings/1/moderation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACTIVE"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/marketplace/payment-proofs"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/admin/marketplace/payment-proofs/1/verify"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/admin/marketplace/payment-proofs/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Invalid proof"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/marketplace/audit-logs"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run RBAC tests**

Run:

```bash
./mvnw test -Dtest=MarketplaceAdminRbacIntegrationTest
```

Expected: PASS if `SecurityConfig` already restricts `/api/v1/admin/**` to ADMIN.

---

## Task 7: Verify Full Marketplace Admin Scope

**Files:**
- All files changed in Tasks 1-6

- [ ] **Step 1: Run focused marketplace admin tests**

Run:

```bash
./mvnw test -Dtest=MarketplaceAdminListingApiIntegrationTest,MarketplaceAdminPaymentProofApiIntegrationTest,MarketplaceAdminAuditLogApiIntegrationTest,MarketplaceAdminRbacIntegrationTest,PaymentProofIntegrationTest,MarketplaceAdminControllerIntegrationTest,MarketplaceSecurityTest
```

Expected: PASS.

- [ ] **Step 2: Run full test suite**

Run:

```bash
./mvnw test
```

Expected: PASS.

- [ ] **Step 3: Run code review agent**

Use `code-reviewer` with this prompt:

```text
Review the Admin Marketplace section 1.8 implementation. Focus on correctness of listing moderation, payment proof verification, marketplace audit logs, RBAC, pagination/filter behavior, validation, and whether the implementation follows existing Spring Boot patterns. Report CRITICAL/HIGH/MEDIUM/LOW findings with exact file paths.
```

- [ ] **Step 4: Run security review agent**

Use `security-reviewer` with this prompt:

```text
Security-review the new /api/v1/admin/marketplace/** APIs. Focus on admin-only authorization, user input validation, JPA query injection safety, audit log data exposure, notification content, and error-message leakage. Report CRITICAL/HIGH findings first with exact file paths.
```

- [ ] **Step 5: Fix review findings and rerun focused tests**

Run:

```bash
./mvnw test -Dtest=MarketplaceAdminListingApiIntegrationTest,MarketplaceAdminPaymentProofApiIntegrationTest,MarketplaceAdminAuditLogApiIntegrationTest,MarketplaceAdminRbacIntegrationTest
```

Expected: PASS.

---

## Self-Review

- Spec coverage: every requested listing moderation, payment proof verification, and audit history endpoint is assigned to a task; each business rule has a test or service implementation step; all required RBAC/integration/audit tests are included.
- Placeholder scan: no `TBD`, `TODO`, or unspecified implementation steps remain; where current code must be inspected (`AuditLogService` repository names), the exact required method signature and query behavior are specified.
- Type consistency: endpoint paths use `/api/v1/admin/marketplace/**`; listing status uses `MarketplaceProductStatus.ACTIVE` / `REJECTED`; proof status uses `MarketplacePaymentVerificationStatus`; pagination uses `PageResponse` throughout.
