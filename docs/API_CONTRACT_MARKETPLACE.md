# Marketplace API Contract

Generated from the Spring MVC controllers and DTOs under `src/main/java/org/example/QuanLyMuaVu/module/marketplace` plus marketplace rules in `SecurityConfig`.

Base URL examples assume the backend runs at `http://localhost:8080`.

## Common conventions

### Authentication

Use JWT bearer auth for protected endpoints:

```http
Authorization: Bearer <jwt>
```

Role requirements are enforced by `SecurityConfig`:

| Route pattern | Auth requirement |
| --- | --- |
| `GET /api/v1/marketplace/products/**` | Public |
| `GET /api/v1/marketplace/farms/**` | Public |
| `GET /api/v1/marketplace/traceability/**` | Public |
| `/api/v1/marketplace/cart/**` | BUYER |
| `/api/v1/marketplace/orders/**` | BUYER |
| `/api/v1/marketplace/addresses/**` | BUYER |
| `/api/v1/marketplace/reviews/**` | BUYER |
| `/api/v1/buyer/orders/**` | BUYER |
| `/api/v1/buyer/addresses/**` | BUYER |
| `/api/v1/buyer/**` | BUYER |
| `/api/v1/marketplace/farmer/**` | FARMER |
| `/api/v1/farmer/**` | FARMER |
| `/api/v1/marketplace/admin/**` | ADMIN |
| `/api/v1/admin/**` | ADMIN |

### Response envelope

All endpoints return `ApiResponse<T>`:

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "OK",
  "result": {}
}
```

For delete endpoints returning no payload, `result` is `null`.

### Pagination shape

Paginated endpoints return `PageResponse<T>` in `result`:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### Enums

`MarketplaceProductStatus`:

```text
DRAFT, PENDING_REVIEW, ACTIVE, REJECTED, INACTIVE, SOLD_OUT, PUBLISHED, HIDDEN
```

`PUBLISHED` and `HIDDEN` are deprecated legacy values; prefer `ACTIVE` and `INACTIVE`.

`MarketplaceOrderStatus`:

```text
PENDING_PAYMENT, PAYMENT_SUBMITTED, PAYMENT_VERIFIED, CONFIRMED, PREPARING, SHIPPED, DELIVERED, COMPLETED, CANCELLED, REJECTED
```

`MarketplacePaymentMethod`:

```text
COD, BANK_TRANSFER
```

`MarketplacePaymentVerificationStatus`:

```text
NOT_REQUIRED, AWAITING_PROOF, SUBMITTED, VERIFIED, REJECTED
```

### Representative response shapes

#### Product summary

```json
{
  "id": 101,
  "slug": "gao-st25-huu-co-101",
  "name": "Gạo ST25 hữu cơ",
  "category": "RICE",
  "shortDescription": "Gạo thơm đóng túi 5kg",
  "price": 150000,
  "unit": "kg",
  "stockQuantity": 100,
  "availableQuantity": 80,
  "imageUrl": "https://example.com/images/rice.jpg",
  "farmerUserId": 12,
  "farmerDisplayName": "Nguyễn Văn A",
  "farmId": 3,
  "farmName": "Trang trại An Phú",
  "seasonId": 7,
  "seasonName": "Vụ Đông Xuân 2026",
  "lotId": 44,
  "region": "Cần Thơ",
  "traceable": true,
  "ratingAverage": 4.8,
  "ratingCount": 25,
  "status": "ACTIVE",
  "createdAt": "2026-05-06T09:30:00",
  "updatedAt": "2026-05-06T10:00:00"
}
```

#### Product detail

Product detail has all product summary fields plus:

```json
{
  "description": "Mô tả chi tiết sản phẩm",
  "imageUrls": ["https://example.com/images/rice-1.jpg"],
  "traceabilityCode": "TRACE-LOT-44"
}
```

#### Cart

```json
{
  "userId": 20,
  "items": [
    {
      "productId": 101,
      "slug": "gao-st25-huu-co-101",
      "name": "Gạo ST25 hữu cơ",
      "imageUrl": "https://example.com/images/rice.jpg",
      "unitPrice": 150000,
      "quantity": 2,
      "maxQuantity": 80,
      "farmerUserId": 12,
      "traceable": true
    }
  ],
  "sellerGroups": [
    {
      "farmerUserId": 12,
      "farmerName": "Nguyễn Văn A",
      "farmId": 3,
      "farmName": "Trang trại An Phú",
      "items": [],
      "subtotal": 300000
    }
  ],
  "itemCount": 2,
  "subtotal": 300000,
  "currency": "VND"
}
```

#### Order

```json
{
  "id": 501,
  "orderCode": "ORD-20260506-0001",
  "orderGroupCode": "GRP-20260506-0001",
  "buyerUserId": 20,
  "farmerUserId": 12,
  "status": "PENDING_PAYMENT",
  "payment": {
    "method": "BANK_TRANSFER",
    "verificationStatus": "AWAITING_PROOF",
    "proofFileName": null,
    "proofContentType": null,
    "proofStoragePath": null,
    "proofUploadedAt": null,
    "verifiedAt": null,
    "verifiedBy": null,
    "verificationNote": null
  },
  "shippingRecipientName": "Trần Thị B",
  "shippingPhone": "0912345678",
  "shippingAddressLine": "12 Nguyễn Trãi, Phường 1, Quận 5, TP.HCM",
  "note": "Giao giờ hành chính",
  "subtotal": 300000,
  "shippingFee": 20000,
  "totalAmount": 320000,
  "canCancel": true,
  "createdAt": "2026-05-06T10:10:00",
  "updatedAt": "2026-05-06T10:10:00",
  "items": [
    {
      "id": 9001,
      "productId": 101,
      "productName": "Gạo ST25 hữu cơ",
      "productSlug": "gao-st25-huu-co-101",
      "imageUrl": "https://example.com/images/rice.jpg",
      "unitPriceSnapshot": 150000,
      "quantity": 2,
      "lineTotal": 300000,
      "traceableSnapshot": true,
      "canReview": false,
      "reviewId": null
    }
  ]
}
```

#### Address

```json
{
  "id": 77,
  "userId": 20,
  "fullName": "Trần Thị B",
  "phone": "0912345678",
  "province": "TP.HCM",
  "district": "Quận 5",
  "ward": "Phường 1",
  "street": "12 Nguyễn Trãi",
  "detail": "Tầng 2",
  "label": "Nhà",
  "isDefault": true
}
```

#### Review

```json
{
  "id": 301,
  "productId": 101,
  "orderId": 501,
  "orderItemId": 9001,
  "buyerUserId": 20,
  "buyerDisplayName": "Trần Thị B",
  "rating": 5,
  "comment": "Sản phẩm chất lượng tốt",
  "hidden": false,
  "createdAt": "2026-05-06T12:00:00",
  "updatedAt": "2026-05-06T12:00:00"
}
```

## Public marketplace catalog APIs

### List products

```http
GET /api/v1/marketplace/products
```

Auth: Public.

Query params:

| Name | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `category` | string | No |  | Category filter. |
| `q` | string | No |  | Search text. |
| `region` | string | No |  | Region filter. |
| `traceable` | boolean | No |  | Traceability filter. |
| `minPrice` | decimal | No |  | Minimum price. |
| `maxPrice` | decimal | No |  | Maximum price. |
| `sort` | string | No | created newest | Supported by service: `price_asc`, `price_desc`; anything else sorts by `createdAt` descending. |
| `page` | integer | No | `0` | Zero-based page. |
| `size` | integer | No | `20` | Page size, coerced to at least 1 by service. |

Response: `ApiResponse<PageResponse<MarketplaceProductSummaryResponse>>`.

Example:

```http
GET /api/v1/marketplace/products?category=RICE&region=C%E1%BA%A7n%20Th%C6%A1&traceable=true&sort=price_asc&page=0&size=12
```

### Get product by slug

```http
GET /api/v1/marketplace/products/{slug}
```

Auth: Public.

Path params:

| Name | Type | Notes |
| --- | --- | --- |
| `slug` | string | Product slug. |

Response: `ApiResponse<MarketplaceProductDetailResponse>`.

### List product reviews

```http
GET /api/v1/marketplace/products/{productId}/reviews
```

Auth: Public.

Path params:

| Name | Type |
| --- | --- |
| `productId` | long |

Query params:

| Name | Type | Required | Default |
| --- | --- | --- | --- |
| `page` | integer | No | `0` |
| `size` | integer | No | `20` |

Response: `ApiResponse<PageResponse<MarketplaceReviewResponse>>`.

### List farms

```http
GET /api/v1/marketplace/farms
```

Auth: Public.

Query params:

| Name | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `q` | string | No |  | Search text. |
| `region` | string | No |  | Region filter. |
| `page` | integer | No | `0` | Zero-based page. |
| `size` | integer | No | `20` | Page size. |

Response: `ApiResponse<PageResponse<MarketplaceFarmSummaryResponse>>`.

Farm summary shape:

```json
{
  "id": 3,
  "name": "Trang trại An Phú",
  "region": "Cần Thơ",
  "address": "Cần Thơ",
  "coverImageUrl": "https://example.com/farm.jpg",
  "productCount": 12
}
```

### Get farm detail

```http
GET /api/v1/marketplace/farms/{farmId}
```

Auth: Public.

Path params:

| Name | Type |
| --- | --- |
| `farmId` | integer |

Response: `ApiResponse<MarketplaceFarmDetailResponse>`.

Farm detail shape:

```json
{
  "id": 3,
  "name": "Trang trại An Phú",
  "region": "Cần Thơ",
  "address": "Cần Thơ",
  "coverImageUrl": "https://example.com/farm.jpg",
  "productCount": 12,
  "description": "Trang trại lúa hữu cơ",
  "ownerUserId": 12,
  "ownerDisplayName": "Nguyễn Văn A",
  "contactPhone": "0911111111"
}
```

### List farm reviews

```http
GET /api/v1/marketplace/farms/{farmId}/reviews
```

Auth: Public.

Path params:

| Name | Type |
| --- | --- |
| `farmId` | integer |

Query params: `page` default `0`, `size` default `20`.

Response: `ApiResponse<PageResponse<MarketplaceReviewResponse>>`.

### Get product traceability

```http
GET /api/v1/marketplace/products/{productId}/traceability
```

Auth: Public, because it matches public `GET /api/v1/marketplace/products/**`.

Path params:

| Name | Type |
| --- | --- |
| `productId` | long |

Response: `ApiResponse<MarketplaceTraceabilityResponse>`.

Traceability shape:

```json
{
  "productId": 101,
  "traceable": true,
  "farm": {
    "id": 3,
    "name": "Trang trại An Phú",
    "region": "Cần Thơ",
    "address": "Cần Thơ",
    "certificationInfo": null
  },
  "plot": {
    "id": 10,
    "name": "Thửa A1",
    "area": 2.5
  },
  "season": {
    "id": 7,
    "name": "Vụ Đông Xuân 2026",
    "cropName": "Lúa",
    "varietyName": "ST25",
    "plantingDate": "2026-01-10",
    "harvestDate": "2026-04-20"
  },
  "harvest": {
    "id": 55,
    "harvestDate": "2026-04-20",
    "quantity": 1200,
    "qualityNotes": "Loại A"
  },
  "productLot": {
    "id": 44,
    "lotCode": "LOT-20260420-001",
    "harvestedAt": "2026-04-20",
    "receivedAt": "2026-04-21T08:00:00",
    "unit": "kg",
    "initialQuantity": 1200,
    "grade": "A",
    "warehouseName": "Kho chính",
    "storageLocation": "Kệ 1"
  },
  "timeline": [
    {
      "milestone": "HARVESTED",
      "date": "2026-04-20T00:00:00",
      "description": "Thu hoạch lô LOT-20260420-001"
    }
  ],
  "validatedAt": "2026-05-06T10:00:00"
}
```

### Get product traceability legacy route

```http
GET /api/v1/marketplace/traceability/{productId}
```

Auth: Public.

Deprecated: use `GET /api/v1/marketplace/products/{productId}/traceability` instead.

Response: `ApiResponse<MarketplaceTraceabilityResponse>`.

## Buyer marketplace APIs

Buyer endpoints are available under two route styles:

- Canonical marketplace routes under `/api/v1/marketplace/...`
- Buyer alias routes under `/api/v1/buyer/...`

Both route styles call the same service methods where documented as aliases.

### Get cart

```http
GET /api/v1/marketplace/cart
```

Auth: BUYER.

Response: `ApiResponse<MarketplaceCartResponse>`.

### Add cart item

```http
POST /api/v1/marketplace/cart/items
Content-Type: application/json
```

Auth: BUYER.

Request body: `MarketplaceAddCartItemRequest`.

Validation:

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `productId` | long | Yes | Not null. |
| `quantity` | decimal | Yes | Greater than `0.0`. |

Example request:

```json
{
  "productId": 101,
  "quantity": 2
}
```

Response: `ApiResponse<MarketplaceCartResponse>`.

### Update cart item quantity

```http
PUT /api/v1/marketplace/cart/items/{productId}
PATCH /api/v1/marketplace/cart/items/{productId}
Content-Type: application/json
```

Auth: BUYER.

Path params: `productId` long.

Request body: `MarketplaceUpdateCartItemRequest`.

Validation:

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `quantity` | decimal | Yes | Greater than `0.0`. |

Example request:

```json
{
  "quantity": 3
}
```

Response: `ApiResponse<MarketplaceCartResponse>`.

### Remove cart item

```http
DELETE /api/v1/marketplace/cart/items/{productId}
```

Auth: BUYER.

Path params: `productId` long.

Response: `ApiResponse<MarketplaceCartResponse>`.

### Merge cart

```http
POST /api/v1/marketplace/cart/merge
Content-Type: application/json
```

Auth: BUYER.

Request body: `MarketplaceMergeCartRequest`.

Validation:

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `items` | array | Yes | Not empty. |
| `items[].productId` | long | Yes | Not null. |
| `items[].quantity` | decimal | Yes | Greater than `0.0`. |

Example request:

```json
{
  "items": [
    { "productId": 101, "quantity": 2 },
    { "productId": 102, "quantity": 1.5 }
  ]
}
```

Response: `ApiResponse<MarketplaceCartResponse>`.

### Clear cart

```http
DELETE /api/v1/marketplace/cart
```

Auth: BUYER.

Response: `ApiResponse<MarketplaceCartResponse>`.

### Preview order

```http
POST /api/v1/marketplace/orders/preview
POST /api/v1/buyer/orders/preview
Content-Type: application/json
```

Auth: BUYER.

Request body: `MarketplaceCreateOrderRequest`.

Validation:

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `paymentMethod` | enum | Yes | `COD` or `BANK_TRANSFER`. |
| `addressId` | long | No | Existing buyer address ID. |
| `shippingRecipientName` | string | No | Manual shipping recipient. |
| `shippingPhone` | string | No | Manual shipping phone. |
| `shippingAddressLine` | string | No | Manual shipping address. |
| `note` | string | No | Buyer note. |
| `idempotencyKey` | string | No | Used by create order, accepted in request DTO. |

Example request:

```json
{
  "paymentMethod": "BANK_TRANSFER",
  "addressId": 77,
  "note": "Giao giờ hành chính"
}
```

Response: `ApiResponse<MarketplaceOrderPreviewResponse>`.

Order preview shape:

```json
{
  "sellerGroups": [
    {
      "farmerUserId": 12,
      "farmerDisplayName": "Nguyễn Văn A",
      "farmId": 3,
      "farmName": "Trang trại An Phú",
      "items": [
        {
          "productId": 101,
          "slug": "gao-st25-huu-co-101",
          "name": "Gạo ST25 hữu cơ",
          "imageUrl": "https://example.com/images/rice.jpg",
          "unitPrice": 150000,
          "quantity": 2,
          "lineTotal": 300000,
          "traceable": true
        }
      ],
      "subtotal": 300000,
      "shippingFee": 20000,
      "groupTotal": 320000
    }
  ],
  "grandSubtotal": 300000,
  "grandShippingFee": 20000,
  "grandTotal": 320000,
  "shippingRecipientName": "Trần Thị B",
  "shippingPhone": "0912345678",
  "shippingAddressLine": "12 Nguyễn Trãi, Phường 1, Quận 5, TP.HCM",
  "totalSellerOrders": 1,
  "currency": "VND"
}
```

### Create order

```http
POST /api/v1/marketplace/orders
POST /api/v1/buyer/orders
Content-Type: application/json
X-Idempotency-Key: <optional-idempotency-key>
```

Auth: BUYER.

Request body: `MarketplaceCreateOrderRequest`.

Idempotency: the service resolves idempotency from the optional `X-Idempotency-Key` header or the optional `idempotencyKey` body field.

Example request:

```json
{
  "paymentMethod": "BANK_TRANSFER",
  "addressId": 77,
  "note": "Giao giờ hành chính",
  "idempotencyKey": "checkout-20-20260506-001"
}
```

Response: `ApiResponse<MarketplaceCreateOrderResultResponse>`.

Create order result shape:

```json
{
  "orderGroupCode": "GRP-20260506-0001",
  "splitCount": 1,
  "orders": []
}
```

`orders` contains `MarketplaceOrderResponse` objects.

### Upload payment proof

```http
POST /api/v1/marketplace/orders/{orderId}/payment-proof
POST /api/v1/buyer/orders/{orderId}/payment-proof
Content-Type: multipart/form-data
```

Auth: BUYER.

Path params: `orderId` long.

Form fields:

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `file` | file | Yes | Multipart file. |

Example curl:

```bash
curl -X POST http://localhost:8080/api/v1/buyer/orders/501/payment-proof \
  -H "Authorization: Bearer <jwt>" \
  -F "file=@bank-transfer.png"
```

Response: `ApiResponse<MarketplaceOrderResponse>`.

### Get payment proof

```http
GET /api/v1/buyer/orders/{orderId}/payment-proof
```

Auth: BUYER.

Path params: `orderId` long.

Response: `ApiResponse<MarketplacePaymentProofResponse>`.

Payment proof shape:

```json
{
  "orderId": 501,
  "orderCode": "ORD-20260506-0001",
  "buyerUserId": 20,
  "proofFileName": "bank-transfer.png",
  "proofContentType": "image/png",
  "proofStoragePath": "marketplace/payment-proofs/501/bank-transfer.png",
  "uploadedAt": "2026-05-06T10:20:00",
  "verificationStatus": "SUBMITTED",
  "verificationNote": null,
  "verifiedAt": null,
  "verifiedBy": null
}
```

### List buyer orders

```http
GET /api/v1/marketplace/orders
GET /api/v1/buyer/orders
```

Auth: BUYER.

Query params:

| Name | Type | Required | Default |
| --- | --- | --- | --- |
| `status` | `MarketplaceOrderStatus` | No |  |
| `page` | integer | No | `0` |
| `size` | integer | No | `20` |

Response: `ApiResponse<PageResponse<MarketplaceOrderResponse>>`.

### Get buyer order detail

```http
GET /api/v1/marketplace/orders/{orderId}
GET /api/v1/buyer/orders/{orderId}
```

Auth: BUYER.

Path params: `orderId` long.

Response: `ApiResponse<MarketplaceOrderResponse>`.

### Cancel buyer order

```http
PUT /api/v1/marketplace/orders/{orderId}/cancel
POST /api/v1/marketplace/orders/{orderId}/cancel
PUT /api/v1/buyer/orders/{orderId}/cancel
POST /api/v1/buyer/orders/{orderId}/cancel
```

Auth: BUYER.

Path params: `orderId` long.

Request body: none.

Response: `ApiResponse<MarketplaceOrderResponse>`.

### Get purchased order item traceability

```http
GET /api/v1/buyer/orders/{orderId}/items/{itemId}/traceability
```

Auth: BUYER.

Path params:

| Name | Type |
| --- | --- |
| `orderId` | long |
| `itemId` | long |

Response: `ApiResponse<MarketplaceTraceabilityResponse>`.

### List buyer addresses

```http
GET /api/v1/marketplace/addresses
GET /api/v1/buyer/addresses
```

Auth: BUYER.

Response: `ApiResponse<List<MarketplaceAddressResponse>>`.

### Create buyer address

```http
POST /api/v1/marketplace/addresses
POST /api/v1/buyer/addresses
Content-Type: application/json
```

Auth: BUYER.

Request body: `MarketplaceAddressUpsertRequest`.

Validation:

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `fullName` | string | Yes | Not blank. |
| `phone` | string | Yes | Vietnamese mobile number matching `^(0|+84)(3|5|7|8|9)[0-9]{8}$`. |
| `province` | string | Yes | Not blank. |
| `district` | string | Yes | Not blank. |
| `ward` | string | Yes | Not blank. |
| `street` | string | Yes | Not blank. |
| `detail` | string | No |  |
| `label` | string | No |  |
| `isDefault` | boolean | No |  |

Example request:

```json
{
  "fullName": "Trần Thị B",
  "phone": "0912345678",
  "province": "TP.HCM",
  "district": "Quận 5",
  "ward": "Phường 1",
  "street": "12 Nguyễn Trãi",
  "detail": "Tầng 2",
  "label": "Nhà",
  "isDefault": true
}
```

Response: `ApiResponse<MarketplaceAddressResponse>`.

### Update buyer address

```http
PATCH /api/v1/marketplace/addresses/{addressId}
PATCH /api/v1/buyer/addresses/{addressId}
Content-Type: application/json
```

Auth: BUYER.

Path params: `addressId` long.

Request body: `MarketplaceAddressUpsertRequest`; same validation as create.

Response: `ApiResponse<MarketplaceAddressResponse>`.

### Set default buyer address

```http
PATCH /api/v1/marketplace/addresses/{addressId}/default
PATCH /api/v1/buyer/addresses/{addressId}/default
```

Auth: BUYER.

Path params: `addressId` long.

Request body: none.

Response: `ApiResponse<MarketplaceAddressResponse>`.

### Delete buyer address

```http
DELETE /api/v1/marketplace/addresses/{addressId}
DELETE /api/v1/buyer/addresses/{addressId}
```

Auth: BUYER.

Path params: `addressId` long.

Response: `ApiResponse<Void>` with `result: null`.

### Create buyer review for an order

```http
POST /api/v1/buyer/orders/{orderId}/reviews
Content-Type: application/json
```

Auth: BUYER.

Path params: `orderId` long.

Request body: `MarketplaceCreateReviewRequest`.

Validation:

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `orderItemId` | long | Yes | Not null. |
| `rating` | integer | Yes | 1 through 5. |
| `comment` | string | No |  |

Example request:

```json
{
  "orderItemId": 9001,
  "rating": 5,
  "comment": "Sản phẩm chất lượng tốt"
}
```

Response: `ApiResponse<MarketplaceReviewResponse>`.

### Edit buyer review

```http
PATCH /api/v1/buyer/reviews/{reviewId}
Content-Type: application/json
```

Auth: BUYER.

Path params: `reviewId` long.

Request body: `MarketplaceUpdateReviewRequest`.

Validation:

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `rating` | integer | No | 1 through 5 when present. |
| `comment` | string | No |  |

Example request:

```json
{
  "rating": 4,
  "comment": "Cập nhật đánh giá"
}
```

Response: `ApiResponse<MarketplaceReviewResponse>`.

### Delete buyer review

```http
DELETE /api/v1/buyer/reviews/{reviewId}
```

Auth: BUYER.

Path params: `reviewId` long.

Response: `ApiResponse<Void>` with `result: null`.

### Create review legacy route

```http
POST /api/v1/marketplace/reviews
Content-Type: application/json
```

Auth: BUYER.

Deprecated: use `POST /api/v1/buyer/orders/{orderId}/reviews` instead.

Request body: `MarketplaceCreateReviewRequest`.

Response: `ApiResponse<MarketplaceReviewResponse>`.

## Farmer marketplace APIs

### Get farmer dashboard

```http
GET /api/v1/marketplace/farmer/dashboard
```

Auth: FARMER.

Response: `ApiResponse<MarketplaceFarmerDashboardResponse>`.

Dashboard shape:

```json
{
  "totalProducts": 10,
  "pendingReviewProducts": 2,
  "publishedProducts": 7,
  "lowStockProducts": 1,
  "pendingOrders": 3,
  "totalRevenue": 1500000,
  "recentOrders": []
}
```

`recentOrders` contains `MarketplaceOrderResponse` objects.

### List farmer products

```http
GET /api/v1/marketplace/farmer/products
```

Auth: FARMER.

Query params:

| Name | Type | Required | Default |
| --- | --- | --- | --- |
| `q` | string | No |  |
| `status` | `MarketplaceProductStatus` | No |  |
| `page` | integer | No | `0` |
| `size` | integer | No | `20` |

Response: `ApiResponse<PageResponse<MarketplaceProductSummaryResponse>>`.

### Get farmer product form options

```http
GET /api/v1/marketplace/farmer/product-form-options
```

Auth: FARMER.

Response: `ApiResponse<MarketplaceFarmerProductFormOptionsResponse>`.

Shape:

```json
{
  "farms": [
    { "id": 3, "name": "Trang trại An Phú" }
  ],
  "seasons": [
    { "id": 7, "seasonName": "Vụ Đông Xuân 2026", "farmId": 3 }
  ],
  "lots": [
    {
      "id": 44,
      "lotCode": "LOT-20260420-001",
      "farmId": 3,
      "farmName": "Trang trại An Phú",
      "seasonId": 7,
      "seasonName": "Vụ Đông Xuân 2026",
      "availableQuantity": 1200,
      "harvestedAt": "2026-04-20",
      "unit": "kg",
      "productName": "Gạo ST25",
      "productVariant": "Hữu cơ",
      "linkedProductId": 101,
      "linkedProductStatus": "ACTIVE"
    }
  ]
}
```

### Get farmer product detail

```http
GET /api/v1/marketplace/farmer/products/{productId}
```

Auth: FARMER.

Path params: `productId` long.

Response: `ApiResponse<MarketplaceProductDetailResponse>`.

### Create farmer product

```http
POST /api/v1/marketplace/farmer/products
Content-Type: application/json
```

Auth: FARMER.

Request body: `MarketplaceFarmerProductUpsertRequest`.

Validation:

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `name` | string | Yes | Not blank. |
| `category` | string | No |  |
| `shortDescription` | string | No |  |
| `description` | string | No |  |
| `price` | decimal | Yes | Greater than `0.0`. |
| `stockQuantity` | decimal | Yes | Greater than `0.0`. |
| `imageUrl` | string | No |  |
| `imageUrls` | string array | No |  |
| `lotId` | integer | Yes | Not null. |

Example request:

```json
{
  "name": "Gạo ST25 hữu cơ",
  "category": "RICE",
  "shortDescription": "Gạo thơm đóng túi 5kg",
  "description": "Sản phẩm canh tác hữu cơ, truy xuất được nguồn gốc.",
  "price": 150000,
  "stockQuantity": 100,
  "imageUrl": "https://example.com/images/rice.jpg",
  "imageUrls": ["https://example.com/images/rice-1.jpg"],
  "lotId": 44
}
```

Response: `ApiResponse<MarketplaceProductDetailResponse>`.

### Update farmer product

```http
PUT /api/v1/marketplace/farmer/products/{productId}
Content-Type: application/json
```

Auth: FARMER.

Path params: `productId` long.

Request body: `MarketplaceFarmerProductUpsertRequest`; same validation as create.

Response: `ApiResponse<MarketplaceProductDetailResponse>`.

### Update farmer product status

```http
PATCH /api/v1/marketplace/farmer/products/{productId}/status
Content-Type: application/json
```

Auth: FARMER.

Path params: `productId` long.

Request body: `MarketplaceUpdateProductStatusRequest`.

Validation:

| Field | Type | Required |
| --- | --- | --- |
| `status` | `MarketplaceProductStatus` | Yes |

Example request:

```json
{
  "status": "PENDING_REVIEW"
}
```

Response: `ApiResponse<MarketplaceProductDetailResponse>`.

### List farmer orders

```http
GET /api/v1/marketplace/farmer/orders
GET /api/v1/farmer/orders
```

Auth: FARMER.

Query params:

| Name | Type | Required | Default |
| --- | --- | --- | --- |
| `status` | `MarketplaceOrderStatus` | No |  |
| `page` | integer | No | `0` |
| `size` | integer | No | `20` |

Response: `ApiResponse<PageResponse<MarketplaceOrderResponse>>`.

### Get farmer order detail

```http
GET /api/v1/marketplace/farmer/orders/{orderId}
GET /api/v1/farmer/orders/{orderId}
```

Auth: FARMER.

Path params: `orderId` long.

Response: `ApiResponse<MarketplaceOrderResponse>`.

### Update farmer order status

```http
PATCH /api/v1/marketplace/farmer/orders/{orderId}/status
PATCH /api/v1/farmer/orders/{orderId}/status
Content-Type: application/json
```

Auth: FARMER.

Path params: `orderId` long.

Request body: `MarketplaceUpdateOrderStatusRequest`.

Validation:

| Field | Type | Required |
| --- | --- | --- |
| `status` | `MarketplaceOrderStatus` | Yes |

Example request:

```json
{
  "status": "PREPARING"
}
```

Response: `ApiResponse<MarketplaceOrderResponse>`.

## Admin marketplace APIs

### List admin products

```http
GET /api/v1/marketplace/admin/products
```

Auth: ADMIN.

Query params:

| Name | Type | Required | Default |
| --- | --- | --- | --- |
| `q` | string | No |  |
| `status` | `MarketplaceProductStatus` | No |  |
| `page` | integer | No | `0` |
| `size` | integer | No | `20` |

Response: `ApiResponse<PageResponse<MarketplaceProductSummaryResponse>>`.

### Update admin product status

```http
PATCH /api/v1/marketplace/admin/products/{productId}/status
Content-Type: application/json
```

Auth: ADMIN.

Path params: `productId` long.

Request body: `MarketplaceUpdateProductStatusRequest`.

Example request:

```json
{
  "status": "ACTIVE"
}
```

Response: `ApiResponse<MarketplaceProductDetailResponse>`.

### List admin orders

```http
GET /api/v1/marketplace/admin/orders
```

Auth: ADMIN.

Query params:

| Name | Type | Required | Default |
| --- | --- | --- | --- |
| `status` | `MarketplaceOrderStatus` | No |  |
| `page` | integer | No | `0` |
| `size` | integer | No | `20` |

Response: `ApiResponse<PageResponse<MarketplaceOrderResponse>>`.

### Get admin order detail

```http
GET /api/v1/marketplace/admin/orders/{orderId}
```

Auth: ADMIN.

Path params: `orderId` long.

Response: `ApiResponse<MarketplaceOrderResponse>`.

### Update admin payment verification

```http
PATCH /api/v1/marketplace/admin/orders/{orderId}/payment-verification
Content-Type: application/json
```

Auth: ADMIN.

Path params: `orderId` long.

Request body: `MarketplaceUpdatePaymentVerificationRequest`.

Validation:

| Field | Type | Required |
| --- | --- | --- |
| `verificationStatus` | `MarketplacePaymentVerificationStatus` | Yes |
| `verificationNote` | string | No |

Example request:

```json
{
  "verificationStatus": "VERIFIED",
  "verificationNote": "Đã đối soát giao dịch ngân hàng"
}
```

Response: `ApiResponse<MarketplaceOrderResponse>`.

### Update admin order status

```http
PATCH /api/v1/marketplace/admin/orders/{orderId}/status
Content-Type: application/json
```

Auth: ADMIN.

Path params: `orderId` long.

Request body: `MarketplaceUpdateOrderStatusRequest`.

Example request:

```json
{
  "status": "CONFIRMED"
}
```

Response: `ApiResponse<MarketplaceOrderResponse>`.

### Get order audit logs

```http
GET /api/v1/marketplace/admin/orders/{orderId}/audit-logs
```

Auth: ADMIN.

Path params: `orderId` long.

Response: `ApiResponse<List<MarketplaceOrderAuditLogResponse>>`.

Audit log shape:

```json
{
  "id": 1,
  "entityType": "MARKETPLACE_ORDER",
  "entityId": 501,
  "operation": "UPDATE_STATUS",
  "performedBy": "admin",
  "performedAt": "2026-05-06T10:30:00",
  "snapshotDataJson": "{...}",
  "reason": "Manual status update",
  "ipAddress": "127.0.0.1"
}
```

### Get admin stats

```http
GET /api/v1/marketplace/admin/stats
```

Auth: ADMIN.

Response: `ApiResponse<MarketplaceAdminStatsResponse>`.

Stats shape:

```json
{
  "totalProducts": 100,
  "pendingReviewProducts": 5,
  "publishedProducts": 80,
  "hiddenProducts": 15,
  "totalOrders": 250,
  "activeOrders": 20,
  "completedOrders": 210,
  "cancelledOrders": 20,
  "totalRevenue": 75000000
}
```

### Hide review

```http
PATCH /api/v1/marketplace/admin/reviews/{reviewId}/hide
```

Auth: ADMIN.

Path params: `reviewId` long.

Request body: none.

Response: `ApiResponse<MarketplaceReviewResponse>`.

### Delete review as admin

```http
DELETE /api/v1/marketplace/admin/reviews/{reviewId}
```

Auth: ADMIN.

Path params: `reviewId` long.

Response: `ApiResponse<Void>` with `result: null`.

## Admin payment proof APIs

These routes are under `/api/v1/admin/**`, so they require ADMIN.

### List payment proofs

```http
GET /api/v1/admin/marketplace/payment-proofs
```

Auth: ADMIN.

Query params:

| Name | Type | Required | Default |
| --- | --- | --- | --- |
| `page` | integer | No | `0` |
| `size` | integer | No | `20` |

Response: `ApiResponse<PageResponse<MarketplacePaymentProofResponse>>`.

### Verify payment proof

```http
PATCH /api/v1/admin/marketplace/payment-proofs/{orderId}/verify
```

Auth: ADMIN.

Path params: `orderId` long.

Request body: none.

Response: `ApiResponse<MarketplacePaymentProofResponse>`.

### Reject payment proof

```http
PATCH /api/v1/admin/marketplace/payment-proofs/{orderId}/reject
Content-Type: application/json
```

Auth: ADMIN.

Path params: `orderId` long.

Request body: `MarketplaceRejectPaymentProofRequest`.

Validation:

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `reason` | string | Yes | Not blank. |

Example request:

```json
{
  "reason": "Ảnh chuyển khoản không rõ mã giao dịch"
}
```

Response: `ApiResponse<MarketplacePaymentProofResponse>`.

## Endpoint inventory

| Method | Path | Auth | Request body | Response result |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/marketplace/products` | Public | None | `PageResponse<MarketplaceProductSummaryResponse>` |
| GET | `/api/v1/marketplace/products/{slug}` | Public | None | `MarketplaceProductDetailResponse` |
| GET | `/api/v1/marketplace/products/{productId}/reviews` | Public | None | `PageResponse<MarketplaceReviewResponse>` |
| GET | `/api/v1/marketplace/farms` | Public | None | `PageResponse<MarketplaceFarmSummaryResponse>` |
| GET | `/api/v1/marketplace/farms/{farmId}` | Public | None | `MarketplaceFarmDetailResponse` |
| GET | `/api/v1/marketplace/farms/{farmId}/reviews` | Public | None | `PageResponse<MarketplaceReviewResponse>` |
| GET | `/api/v1/marketplace/products/{productId}/traceability` | Public | None | `MarketplaceTraceabilityResponse` |
| GET | `/api/v1/marketplace/traceability/{productId}` | Public | None | `MarketplaceTraceabilityResponse` |
| GET | `/api/v1/marketplace/cart` | BUYER | None | `MarketplaceCartResponse` |
| POST | `/api/v1/marketplace/cart/items` | BUYER | `MarketplaceAddCartItemRequest` | `MarketplaceCartResponse` |
| PUT | `/api/v1/marketplace/cart/items/{productId}` | BUYER | `MarketplaceUpdateCartItemRequest` | `MarketplaceCartResponse` |
| PATCH | `/api/v1/marketplace/cart/items/{productId}` | BUYER | `MarketplaceUpdateCartItemRequest` | `MarketplaceCartResponse` |
| DELETE | `/api/v1/marketplace/cart/items/{productId}` | BUYER | None | `MarketplaceCartResponse` |
| POST | `/api/v1/marketplace/cart/merge` | BUYER | `MarketplaceMergeCartRequest` | `MarketplaceCartResponse` |
| DELETE | `/api/v1/marketplace/cart` | BUYER | None | `MarketplaceCartResponse` |
| POST | `/api/v1/marketplace/orders/preview` | BUYER | `MarketplaceCreateOrderRequest` | `MarketplaceOrderPreviewResponse` |
| POST | `/api/v1/marketplace/orders` | BUYER | `MarketplaceCreateOrderRequest` | `MarketplaceCreateOrderResultResponse` |
| POST | `/api/v1/marketplace/orders/{orderId}/payment-proof` | BUYER | Multipart `file` | `MarketplaceOrderResponse` |
| GET | `/api/v1/marketplace/orders` | BUYER | None | `PageResponse<MarketplaceOrderResponse>` |
| GET | `/api/v1/marketplace/orders/{orderId}` | BUYER | None | `MarketplaceOrderResponse` |
| PUT | `/api/v1/marketplace/orders/{orderId}/cancel` | BUYER | None | `MarketplaceOrderResponse` |
| POST | `/api/v1/marketplace/orders/{orderId}/cancel` | BUYER | None | `MarketplaceOrderResponse` |
| GET | `/api/v1/marketplace/addresses` | BUYER | None | `List<MarketplaceAddressResponse>` |
| POST | `/api/v1/marketplace/addresses` | BUYER | `MarketplaceAddressUpsertRequest` | `MarketplaceAddressResponse` |
| PATCH | `/api/v1/marketplace/addresses/{addressId}` | BUYER | `MarketplaceAddressUpsertRequest` | `MarketplaceAddressResponse` |
| PATCH | `/api/v1/marketplace/addresses/{addressId}/default` | BUYER | None | `MarketplaceAddressResponse` |
| DELETE | `/api/v1/marketplace/addresses/{addressId}` | BUYER | None | `Void` |
| POST | `/api/v1/marketplace/reviews` | BUYER | `MarketplaceCreateReviewRequest` | `MarketplaceReviewResponse` |
| POST | `/api/v1/buyer/orders/preview` | BUYER | `MarketplaceCreateOrderRequest` | `MarketplaceOrderPreviewResponse` |
| POST | `/api/v1/buyer/orders` | BUYER | `MarketplaceCreateOrderRequest` | `MarketplaceCreateOrderResultResponse` |
| GET | `/api/v1/buyer/orders` | BUYER | None | `PageResponse<MarketplaceOrderResponse>` |
| GET | `/api/v1/buyer/orders/{orderId}` | BUYER | None | `MarketplaceOrderResponse` |
| PUT | `/api/v1/buyer/orders/{orderId}/cancel` | BUYER | None | `MarketplaceOrderResponse` |
| POST | `/api/v1/buyer/orders/{orderId}/cancel` | BUYER | None | `MarketplaceOrderResponse` |
| POST | `/api/v1/buyer/orders/{orderId}/payment-proof` | BUYER | Multipart `file` | `MarketplaceOrderResponse` |
| GET | `/api/v1/buyer/orders/{orderId}/payment-proof` | BUYER | None | `MarketplacePaymentProofResponse` |
| GET | `/api/v1/buyer/orders/{orderId}/items/{itemId}/traceability` | BUYER | None | `MarketplaceTraceabilityResponse` |
| GET | `/api/v1/buyer/addresses` | BUYER | None | `List<MarketplaceAddressResponse>` |
| POST | `/api/v1/buyer/addresses` | BUYER | `MarketplaceAddressUpsertRequest` | `MarketplaceAddressResponse` |
| PATCH | `/api/v1/buyer/addresses/{addressId}` | BUYER | `MarketplaceAddressUpsertRequest` | `MarketplaceAddressResponse` |
| DELETE | `/api/v1/buyer/addresses/{addressId}` | BUYER | None | `Void` |
| PATCH | `/api/v1/buyer/addresses/{addressId}/default` | BUYER | None | `MarketplaceAddressResponse` |
| POST | `/api/v1/buyer/orders/{orderId}/reviews` | BUYER | `MarketplaceCreateReviewRequest` | `MarketplaceReviewResponse` |
| PATCH | `/api/v1/buyer/reviews/{reviewId}` | BUYER | `MarketplaceUpdateReviewRequest` | `MarketplaceReviewResponse` |
| DELETE | `/api/v1/buyer/reviews/{reviewId}` | BUYER | None | `Void` |
| GET | `/api/v1/marketplace/farmer/dashboard` | FARMER | None | `MarketplaceFarmerDashboardResponse` |
| GET | `/api/v1/marketplace/farmer/products` | FARMER | None | `PageResponse<MarketplaceProductSummaryResponse>` |
| GET | `/api/v1/marketplace/farmer/product-form-options` | FARMER | None | `MarketplaceFarmerProductFormOptionsResponse` |
| GET | `/api/v1/marketplace/farmer/products/{productId}` | FARMER | None | `MarketplaceProductDetailResponse` |
| POST | `/api/v1/marketplace/farmer/products` | FARMER | `MarketplaceFarmerProductUpsertRequest` | `MarketplaceProductDetailResponse` |
| PUT | `/api/v1/marketplace/farmer/products/{productId}` | FARMER | `MarketplaceFarmerProductUpsertRequest` | `MarketplaceProductDetailResponse` |
| PATCH | `/api/v1/marketplace/farmer/products/{productId}/status` | FARMER | `MarketplaceUpdateProductStatusRequest` | `MarketplaceProductDetailResponse` |
| GET | `/api/v1/marketplace/farmer/orders` | FARMER | None | `PageResponse<MarketplaceOrderResponse>` |
| GET | `/api/v1/marketplace/farmer/orders/{orderId}` | FARMER | None | `MarketplaceOrderResponse` |
| PATCH | `/api/v1/marketplace/farmer/orders/{orderId}/status` | FARMER | `MarketplaceUpdateOrderStatusRequest` | `MarketplaceOrderResponse` |
| GET | `/api/v1/farmer/orders` | FARMER | None | `PageResponse<MarketplaceOrderResponse>` |
| GET | `/api/v1/farmer/orders/{orderId}` | FARMER | None | `MarketplaceOrderResponse` |
| PATCH | `/api/v1/farmer/orders/{orderId}/status` | FARMER | `MarketplaceUpdateOrderStatusRequest` | `MarketplaceOrderResponse` |
| GET | `/api/v1/marketplace/admin/products` | ADMIN | None | `PageResponse<MarketplaceProductSummaryResponse>` |
| PATCH | `/api/v1/marketplace/admin/products/{productId}/status` | ADMIN | `MarketplaceUpdateProductStatusRequest` | `MarketplaceProductDetailResponse` |
| GET | `/api/v1/marketplace/admin/orders` | ADMIN | None | `PageResponse<MarketplaceOrderResponse>` |
| GET | `/api/v1/marketplace/admin/orders/{orderId}` | ADMIN | None | `MarketplaceOrderResponse` |
| PATCH | `/api/v1/marketplace/admin/orders/{orderId}/payment-verification` | ADMIN | `MarketplaceUpdatePaymentVerificationRequest` | `MarketplaceOrderResponse` |
| PATCH | `/api/v1/marketplace/admin/orders/{orderId}/status` | ADMIN | `MarketplaceUpdateOrderStatusRequest` | `MarketplaceOrderResponse` |
| GET | `/api/v1/marketplace/admin/orders/{orderId}/audit-logs` | ADMIN | None | `List<MarketplaceOrderAuditLogResponse>` |
| GET | `/api/v1/marketplace/admin/stats` | ADMIN | None | `MarketplaceAdminStatsResponse` |
| PATCH | `/api/v1/marketplace/admin/reviews/{reviewId}/hide` | ADMIN | None | `MarketplaceReviewResponse` |
| DELETE | `/api/v1/marketplace/admin/reviews/{reviewId}` | ADMIN | None | `Void` |
| GET | `/api/v1/admin/marketplace/payment-proofs` | ADMIN | None | `PageResponse<MarketplacePaymentProofResponse>` |
| PATCH | `/api/v1/admin/marketplace/payment-proofs/{orderId}/verify` | ADMIN | None | `MarketplacePaymentProofResponse` |
| PATCH | `/api/v1/admin/marketplace/payment-proofs/{orderId}/reject` | ADMIN | `MarketplaceRejectPaymentProofRequest` | `MarketplacePaymentProofResponse` |