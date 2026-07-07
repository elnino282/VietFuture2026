# Hướng dẫn triển khai Giai đoạn 3 & 4 — VietFuture2026

> **Mục tiêu:** Triển khai 6 modules mới vào project VietFuture2026 một cách có hệ thống, không xung đột với code hiện tại.
>
> **Tiền đề:** Đã nắm vững codebase (12 microservices, React frontend, Spring Boot, RabbitMQ, MySQL).

---

## Mục lục

1. [Nguyên tắc chung](#nguyên-tắc-chung)
2. [Module 3.1 — Quản lý Tài liệu Nông trại](#module-31--quản-lý-tài-liệu-nông-trại)
3. [Module 3.2 — Chứng minh Đạt chuẩn VietGAP](#module-32--chứng-minh-đạt-chuẩn-vietgap)
4. [Module 3.3 — Tính toán PHI](#module-33--tính-toán-phi)
5. [Module 3.4 — Minh bạch Người tiêu dùng](#module-34--minh-bạch-người-tiêu-dùng)
6. [Module 4.1 — Tối ưu UI/UX](#module-41--tối-ưu-uiux)
7. [Module 4.2 — Tích hợp Giao hàng](#module-42--tích-hợp-giao-hàng)
8. [Checklist trước khi deploy](#checklist-trước-khi-deploy)

---

## Nguyên tắc chung

### Quy tắc vàng

1. **Additive Only** — Mọi thay đổi chỉ thêm mới, không sửa entity/API đã có. Nếu bắt buộc phải sửa, confirm kỹ với team.
2. **Domain Boundary** — Mỗi module thuộc đúng 1 service, không crossover:
   - Farm Document → `farm-service`
   - Certification → `farm-service` + `sustainability-service`
   - PHI → `season-service`
   - Public Traceability → `marketplace-service`
   - Delivery → **service mới** `delivery-service`
   - UI/UX → `agricultural-crop-management-frontend`
3. **Migration thứ tự** — V4 → V5 → V6 → V7 → V8. Không skip số.
4. **Test sau mỗi module** — Run Docker stack, kiểm tra API mới trước khi qua module tiếp theo.
5. **Feature Flag** — Mỗi module mới có config flag trong `application.yml`:

```yaml
# farm-service/src/main/resources/application.yml
farm:
  certification:
    enabled: false   # bật true sau khi module 3.2 hoàn tất
  document:
    enabled: true     # bật true sau khi module 3.1 hoàn tất

# season-service/src/main/resources/application.yml
season:
  phi:
    enabled: false   # bật true sau khi module 3.3 hoàn tất
```

---

## Module 3.1 — Quản lý Tài liệu Nông trại

> **Tuần 1-2** | Service: `farm-service` | Database: `farm_db`

### Mục tiêu

Mỗi nông trại có bộ tài liệu riêng phục vụ kiểm tra VietGAP: giấy phép đất, báo cáo kiểm tra đất/nước, nhật ký phân bón/thuốc, hồ sơ thu hoạch.

### Bước 1: Tạo Flyway Migration

Tạo file:

```
farm-service/src/main/resources/db/migration/V4__farm_documents.sql
```

```sql
CREATE TABLE farm_documents (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    farm_id         INT          NOT NULL,
    document_type   VARCHAR(50)   NOT NULL COMMENT 'LAND_CERTIFICATE, SOIL_TEST_REPORT, WATER_TEST_REPORT, PESTICIDE_RECORD, FERTILIZER_RECORD, HARVEST_LOG, INTERNAL_AUDIT, CERTIFICATE, OTHER',
    title           VARCHAR(255)  NOT NULL,
    description     TEXT,
    file_url        VARCHAR(1000) NULL     COMMENT 'MinIO object URL',
    issued_date     DATE          NULL,
    expiry_date     DATE          NULL,
    verification_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, VERIFIED, REJECTED, EXPIRED',
    verified_by     BIGINT        NULL,
    verified_at     DATETIME      NULL,
    created_by      BIGINT        NOT NULL,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_farm_documents_farm_id (farm_id),
    INDEX idx_farm_documents_type (document_type),
    INDEX idx_farm_documents_status (verification_status),
    INDEX idx_farm_documents_expiry (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Bước 2: Tạo Entity

Tạo file:

```
farm-service/src/main/java/org/example/farm/entity/FarmDocument.java
```

```java
package org.example.farm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "farm_documents")
public class FarmDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "farm_id", nullable = false)
    Integer farmId;

    @Column(name = "document_type", nullable = false, length = 50)
    String documentType;

    @Column(nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "file_url", length = 1000)
    String fileUrl;

    @Column(name = "issued_date")
    LocalDate issuedDate;

    @Column(name = "expiry_date")
    LocalDate expiryDate;

    @Column(name = "verification_status", length = 20)
    @Builder.Default
    String verificationStatus = "PENDING";

    @Column(name = "verified_by")
    Long verifiedBy;

    @Column(name = "verified_at")
    LocalDateTime verifiedAt;

    @Column(name = "created_by", nullable = false)
    Long createdBy;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
    @PreUpdate void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### Bước 3: Tạo Enum DocumentType

Tạo file:

```
farm-service/src/main/java/org/example/farm/enums/FarmDocumentType.java
```

```java
package org.example.farm.enums;

public enum FarmDocumentType {
    LAND_CERTIFICATE,      // Giấy phép đất
    SOIL_TEST_REPORT,      // Báo cáo phân tích đất
    WATER_TEST_REPORT,      // Báo cáo phân tích nguồn nước
    PESTICIDE_RECORD,      // Hồ sơ thuốc BVTV
    FERTILIZER_RECORD,      // Hồ sơ phân bón
    HARVEST_LOG,            // Hồ sơ thu hoạch
    INTERNAL_AUDIT,         // Biên bản kiểm tra nội bộ
    CERTIFICATE,            // Giấy chứng nhận (VietGAP, Organic, GlobalGAP)
    OTHER                   // Khác
}
```

### Bước 4: Tạo Repository

Tạo file:

```
farm-service/src/main/java/org/example/farm/repository/FarmDocumentRepository.java
```

```java
package org.example.farm.repository;

import org.example.farm.entity.FarmDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface FarmDocumentRepository extends JpaRepository<FarmDocument, Integer> {

    List<FarmDocument> findByFarmId(Integer farmId);

    Page<FarmDocument> findByFarmId(Integer farmId, Pageable pageable);

    List<FarmDocument> findByFarmIdAndDocumentType(Integer farmId, String documentType);

    // Tài liệu sắp hết hạn (trong 30 ngày tới)
    @Query("SELECT d FROM FarmDocument d WHERE d.farmId = :farmId " +
           "AND d.expiryDate IS NOT NULL " +
           "AND d.expiryDate BETWEEN :today AND :deadline " +
           "ORDER BY d.expiryDate ASC")
    List<FarmDocument> findExpiringDocuments(
        @Param("farmId") Integer farmId,
        @Param("today") LocalDate today,
        @Param("deadline") LocalDate deadline);

    long countByFarmIdAndVerificationStatus(Integer farmId, String status);
}
```

### Bước 5: Tạo DTOs

Tạo request DTO:

```
farm-service/src/main/java/org/example/farm/dto/request/FarmDocumentCreateRequest.java
```

```java
package org.example.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record FarmDocumentCreateRequest(
    @NotBlank(message = "Tiêu đề không được trống")
    String title,
    @NotNull(message = "Loại tài liệu không được trống")
    String documentType,
    String description,
    String fileUrl,
    LocalDate issuedDate,
    LocalDate expiryDate
) {}
```

Tạo response DTO:

```
farm-service/src/main/java/org/example/farm/dto/response/FarmDocumentResponse.java
```

```java
package org.example.farm.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FarmDocumentResponse(
    Integer id,
    Integer farmId,
    String documentType,
    String documentTypeLabel,  // label tiếng Việt
    String title,
    String description,
    String fileUrl,
    LocalDate issuedDate,
    LocalDate expiryDate,
    boolean isExpired,
    boolean isExpiringSoon,   // sắp hết hạn trong 30 ngày
    String verificationStatus,
    String verifiedByName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

### Bước 6: Tạo Service

Tạo file:

```
farm-service/src/main/java/org/example/farm/service/FarmDocumentService.java
```

```java
package org.example.farm.service;

import lombok.RequiredArgsConstructor;
import org.example.farm.dto.request.FarmDocumentCreateRequest;
import org.example.farm.dto.response.FarmDocumentResponse;
import org.example.farm.entity.FarmDocument;
import org.example.farm.repository.FarmDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FarmDocumentService {

    private final FarmDocumentRepository repository;

    @Transactional
    public FarmDocumentResponse create(Integer farmId, Long userId,
                                       FarmDocumentCreateRequest req) {
        FarmDocument doc = FarmDocument.builder()
            .farmId(farmId)
            .documentType(req.documentType())
            .title(req.title())
            .description(req.description())
            .fileUrl(req.fileUrl())
            .issuedDate(req.issuedDate())
            .expiryDate(req.expiryDate())
            .verificationStatus("PENDING")
            .createdBy(userId)
            .build();
        return toResponse(repository.save(doc));
    }

    @Transactional(readOnly = true)
    public List<FarmDocumentResponse> getByFarmId(Integer farmId) {
        return repository.findByFarmId(farmId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FarmDocumentResponse> getExpiringDocuments(Integer farmId) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(30);
        return repository.findExpiringDocuments(farmId, today, deadline)
            .stream().map(this::toResponse).toList();
    }

    // === Mapping ===
    private FarmDocumentResponse toResponse(FarmDocument d) {
        LocalDate today = LocalDate.now();
        boolean isExpired = d.getExpiryDate() != null && d.getExpiryDate().isBefore(today);
        boolean expiringSoon = d.getExpiryDate() != null
            && !isExpired
            && d.getExpiryDate().isBefore(today.plusDays(30));

        return new FarmDocumentResponse(
            d.getId(), d.getFarmId(), d.getDocumentType(),
            getLabel(d.getDocumentType()), d.getTitle(),
            d.getDescription(), d.getFileUrl(),
            d.getIssuedDate(), d.getExpiryDate(),
            isExpired, expiringSoon,
            d.getVerificationStatus(), null, // verifiedByName
            d.getCreatedAt(), d.getUpdatedAt()
        );
    }

    private String getLabel(String type) {
        return switch (type) {
            case "LAND_CERTIFICATE"      -> "Giấy phép đất";
            case "SOIL_TEST_REPORT"       -> "Báo cáo phân tích đất";
            case "WATER_TEST_REPORT"       -> "Báo cáo phân tích nguồn nước";
            case "PESTICIDE_RECORD"        -> "Hồ sơ thuốc BVTV";
            case "FERTILIZER_RECORD"       -> "Hồ sơ phân bón";
            case "HARVEST_LOG"            -> "Hồ sơ thu hoạch";
            case "INTERNAL_AUDIT"          -> "Biên bản kiểm tra nội bộ";
            case "CERTIFICATE"            -> "Giấy chứng nhận";
            default                       -> "Khác";
        };
    }
}
```

### Bước 7: Tạo Controller

Tạo file:

```
farm-service/src/main/java/org/example/farm/controller/FarmDocumentController.java
```

```java
package org.example.farm.controller;

import lombok.RequiredArgsConstructor;
import org.example.farm.dto.request.FarmDocumentCreateRequest;
import org.example.farm.dto.response.FarmDocumentResponse;
import org.example.farm.service.FarmDocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/farms/{farmId}/documents")
@RequiredArgsConstructor
public class FarmDocumentController {

    private final FarmDocumentService service;

    @PostMapping
    public ResponseEntity<FarmDocumentResponse> create(
            @PathVariable Integer farmId,
            @RequestBody FarmDocumentCreateRequest req,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.create(farmId, userId, req));
    }

    @GetMapping
    public ResponseEntity<List<FarmDocumentResponse>> list(
            @PathVariable Integer farmId) {
        return ResponseEntity.ok(service.getByFarmId(farmId));
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<FarmDocumentResponse>> expiring(
            @PathVariable Integer farmId) {
        return ResponseEntity.ok(service.getExpiringDocuments(farmId));
    }
}
```

### Bước 8: Thêm Route vào API Gateway

Thêm vào `api-gateway/src/main/resources/application.yml`:

```yaml
- id: farm-service
  uri: http://farm-service:8084
  predicates:
    - Path=/api/v1/farms/**
  filters:
    - StripPrefix=0
```

### Bước 9: Tạo Frontend Page

Tạo file mới:

```
agricultural-crop-management-frontend/src/pages/farmer/FarmDocumentsPage.tsx
```

Component cần có:
- **Bảng danh sách tài liệu** — phân loại theo `documentType`, hiển thị trạng thái xác minh, ngày hết hạn
- **Upload file** — dùng `multipart/form-data`, upload lên MinIO qua API farm-service
- **Filter theo loại** — dropdown filter VietGAP document categories
- **Badge expiry** — màu đỏ nếu hết hạn, màu vàng nếu sắp hết (30 ngày)
- **Notification** — gọi `GET /expiring` khi load trang, hiển thị toast warning

```tsx
// Cấu trúc gợi ý (pseudo-code)
export function FarmDocumentsPage() {
  const { farmId } = useFarmerFarmContext(); // lấy farmId từ context
  const { data: documents } = useQuery({
    queryKey: ['farm-documents', farmId],
    queryFn: () => api.get(`/farms/${farmId}/documents`),
  });
  const { data: expiring } = useQuery({
    queryKey: ['farm-documents', farmId, 'expiring'],
    queryFn: () => api.get(`/farms/${farmId}/documents/expiring`),
  });

  // Alert nếu có tài liệu sắp hết hạn
  useEffect(() => {
    if (expiring?.length > 0) {
      toast.warning(`${expiring.length} tài liệu sắp hết hạn trong 30 ngày`);
    }
  }, [expiring]);

  // ... render bảng, upload, filter
}
```

### Bước 10: Thêm Navigation

Thêm route vào `app/routes.tsx`:

```tsx
{
  path: '/farmer/farm-documents',
  element: <FarmDocumentsPage />,
}
```

Thêm menu item vào farmer sidebar.

### Checklist test Module 3.1

- [ ] `POST /api/v1/farms/1/documents` → 201 Created
- [ ] `GET /api/v1/farms/1/documents` → list documents
- [ ] `GET /api/v1/farms/1/documents/expiring` → expiring docs
- [ ] Frontend: upload file → hiển thị trong bảng
- [ ] Frontend: toast warning khi có tài liệu sắp hết hạn

---

## Module 3.2 — Chứng minh Đạt chuẩn VietGAP

> **Tuần 5-7** | Service: `farm-service` + `sustainability-service` | Database: `farm_db`

### Mục tiêu

Engine tự động đánh giá compliance score của nông trại dựa trên dữ liệu hiện có. Farmer thấy được checklist VietGAP với trạng thái PASS/FAIL/PENDING. Khi compliance ≥ 80% → enable nộp đơn xin chứng nhận.

### Bước 1: Tạo Migration

```
farm-service/src/main/resources/db/migration/V6__certification.sql
```

```sql
-- Bảng tiêu chuẩn chứng nhận
CREATE TABLE certification_standards (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    code         VARCHAR(50)  NOT NULL UNIQUE,
    name         VARCHAR(255) NOT NULL,
    type         VARCHAR(30)  NOT NULL COMMENT 'VIETGAP_PLANTING, VIETGAP_AQUACULTURE, ORGANIC, GLOBALGAP',
    version      VARCHAR(20)  DEFAULT '1.0',
    description  TEXT,
    is_active    BOOLEAN     DEFAULT TRUE,
    created_at   DATETIME    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng checklist item
CREATE TABLE certification_checklist_items (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    standard_id      INT          NOT NULL,
    item_code       VARCHAR(50)  NOT NULL,
    category        VARCHAR(50)  NOT NULL COMMENT 'PRODUCTION_AREA, SEED, CULTIVATION, HARVEST, etc.',
    description     TEXT         NOT NULL,
    is_mandatory    BOOLEAN     DEFAULT TRUE,
    weight_pct       DECIMAL(5,2) DEFAULT 1.00 COMMENT 'Trọng số trong tính compliance score',
    data_source_type VARCHAR(30)  NULL COMMENT 'FIELD_LOG, SOIL_TEST, WATER_TEST, DISEASE_RECORD, MANUAL',
    data_source_query VARCHAR(500) NULL COMMENT 'Query pattern để auto-fill',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_items_standard (standard_id),
    INDEX idx_items_category (category),
    UNIQUE KEY uk_standard_item (standard_id, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng record chứng nhận của từng farm
CREATE TABLE certification_records (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    farm_id           INT          NOT NULL,
    standard_id       INT          NOT NULL,
    compliance_score  DECIMAL(5,2) NULL COMMENT 'Tính tự động từ checklist items',
    status            VARCHAR(20)   DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS, READY_TO_APPLY, APPLIED, CERTIFIED, REJECTED, EXPIRED',
    applied_at        DATETIME     NULL,
    certified_at      DATETIME     NULL,
    expiry_date       DATE         NULL,
    auditor_notes      TEXT         NULL,
    created_at        DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_records_farm (farm_id),
    INDEX idx_records_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng trạng thái từng checklist item của farm
CREATE TABLE certification_item_statuses (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    record_id      INT          NOT NULL,
    checklist_item_id INT        NOT NULL,
    status         VARCHAR(20)  DEFAULT 'PENDING' COMMENT 'PASS, FAIL, PENDING, NOT_APPLICABLE',
    evidence_url   VARCHAR(1000) NULL,
    notes          TEXT         NULL,
    checked_at     DATETIME     NULL,
    checked_by     BIGINT       NULL,
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_record_item (record_id, checklist_item_id),
    INDEX idx_item_statuses_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Bước 2: Seed Data — Checklist VietGAP Trồng trọt

Tạo seed data migration:

```
farm-service/src/main/resources/db/migration/V6__certification_seed.sql
```

```sql
-- Seed tiêu chuẩn VietGAP Trồng trọt TCVN 11892-1:2017
INSERT INTO certification_standards (code, name, type, description) VALUES
('VIETGAP-PLANTING-2024', 'VietGAP Trồng trọt 2024', 'VIETGAP_PLANTING',
 'Thực hành nông nghiệp tốt cho trồng trọt theo TCVN 11892-1:2017');

-- Seed checklist items (sample — 4 nhóm chính)
INSERT INTO certification_checklist_items (standard_id, item_code, category, description, is_mandatory, weight_pct, data_source_type, data_source_query) VALUES
-- NHÓM 1: Vùng sản xuất
(1, 'PA-001', 'PRODUCTION_AREA', 'Đất sản xuất không bị ô nhiễm, có kết quả phân tích đất trong vòng 12 tháng', TRUE, 5.00, 'SOIL_TEST', '{"seasonId": null, "freshnessDays": 365}'),
(1, 'PA-002', 'PRODUCTION_AREA', 'Nguồn nước tưới đạt QCVN 08-MT:2015/BTNMT', TRUE, 5.00, 'WATER_TEST', '{"seasonId": null, "freshnessDays": 365}'),
(1, 'PA-003', 'PRODUCTION_AREA', 'Vùng sản xuất có sơ đồ mặt bằng rõ ràng', TRUE, 3.00, 'MANUAL', NULL),

-- NHÓM 2: Giống cây trồng
(1, 'SE-001', 'SEED', 'Sử dụng giống có nguồn gốc rõ ràng, có giấy chứng nhận nguồn giống', TRUE, 4.00, 'FIELD_LOG', '{"logType": "SEEDING"}'),
(1, 'SE-002', 'SEED', 'Ghi chép ngày gieo trồng và nguồn giống', TRUE, 2.00, 'FIELD_LOG', '{"logType": "SEEDING"}'),

-- NHÓM 3: Canh tác
(1, 'CU-001', 'CULTIVATION', 'Ghi chép đầy đủ phân bón đã sử dụng (loại, lượng, ngày)', TRUE, 5.00, 'FIELD_LOG', '{"logType": "FERTILIZER_APPLICATION"}'),
(1, 'CU-002', 'CULTIVATION', 'Ghi chép đầy đủ thuốc BVTV đã sử dụng (tên, ngày phun, PHI)', TRUE, 5.00, 'FIELD_LOG', '{"logType": "PESTICIDE_APPLICATION"}'),
(1, 'CU-003', 'CULTIVATION', 'Tuân thủ thời gian cách ly (PHI) trước thu hoạch', TRUE, 5.00, 'PHI_CHECK', NULL),

-- NHÓM 4: Thu hoạch & Bảo quản
(1, 'HV-001', 'HARVEST', 'Thu hoạch đúng thời điểm, có nhật ký thu hoạch', TRUE, 3.00, 'FIELD_LOG', '{"logType": "HARVEST"}'),
(1, 'HV-002', 'HARVEST', 'Sản phẩm sau thu hoạch được bảo quản đúng cách, có hồ sơ kho', TRUE, 2.00, 'MANUAL', NULL);
```

### Bước 3: Entity Classes

Tạo các entity:

```
farm-service/src/main/java/org/example/farm/entity/CertificationStandard.java
farm-service/src/main/java/org/example/farm/entity/CertificationChecklistItem.java
farm-service/src/main/java/org/example/farm/entity/CertificationRecord.java
farm-service/src/main/java/org/example/farm/entity/CertificationItemStatus.java
```

Entity mẫu `CertificationStandard`:

```java
package org.example.farm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "certification_standards")
public class CertificationStandard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(nullable = false, unique = true)
    String code;

    @Column(nullable = false)
    String name;

    @Column(nullable = false, length = 30)
    String type;

    @Column(length = 20)
    @Builder.Default
    String version = "1.0";

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "is_active")
    @Builder.Default
    Boolean isActive = true;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}
```

### Bước 4: Certification Scoring Engine

Đây là logic cốt lõi — tự động tính compliance score:

```java
package org.example.farm.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CertificationScoringService {

    /**
     * Tính compliance score dựa trên checklist items.
     * Score = Σ(passed_items.weight) / Σ(all_mandatory_items.weight) × 100
     */
    public BigDecimal calculateScore(List<CertificationItemStatus> statuses,
                                    List<CertificationChecklistItem> items) {
        BigDecimal totalMandatoryWeight = BigDecimal.ZERO;
        BigDecimal passedWeight = BigDecimal.ZERO;

        for (CertificationChecklistItem item : items) {
            if (!item.getIsMandatory()) continue;
            totalMandatoryWeight = totalMandatoryWeight.add(
                item.getWeightPct() != null ? item.getWeightPct() : BigDecimal.ONE);

            CertificationItemStatus status = findStatus(statuses, item.getId());
            if (status != null && "PASS".equals(status.getStatus())) {
                passedWeight = passedWeight.add(
                    item.getWeightPct() != null ? item.getWeightPct() : BigDecimal.ONE);
            }
        }

        if (totalMandatoryWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return passedWeight
            .divide(totalMandatoryWeight, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Auto-fill PENDING items dựa trên dữ liệu hiện có.
     * PHI check: nếu không có pesticide record vi phạm → PASS.
     */
    public void autoPopulateFromFieldLogs(List<CertificationItemStatus> pendingStatuses,
                                          FieldLogRepository fieldLogRepo,
                                          PesticideRecordRepository pesticideRepo) {
        for (CertificationItemStatus status : pendingStatuses) {
            CertificationChecklistItem item = status.getChecklistItem();
            if (item.getDataSourceType() == null) continue;

            switch (item.getDataSourceType()) {
                case "SOIL_TEST" -> {
                    // Check: có soil test trong 365 ngày không?
                    var tests = soilTestRepo.findByPlotIdAndFreshness(plotId, 365);
                    if (!tests.isEmpty()) {
                        status.setStatus("PASS");
                        status.setCheckedAt(LocalDateTime.now());
                    }
                }
                case "WATER_TEST" -> {
                    var tests = waterTestRepo.findByPlotIdAndFreshness(plotId, 365);
                    if (!tests.isEmpty()) {
                        status.setStatus("PASS");
                        status.setCheckedAt(LocalDateTime.now());
                    }
                }
                case "PHI_CHECK" -> {
                    // Check: có pesticide record nào harvest_date < harvest_allowed_date không?
                    var violations = pesticideRepo.findPHIViolations(seasonId);
                    if (violations.isEmpty()) {
                        status.setStatus("PASS");
                        status.setCheckedAt(LocalDateTime.now());
                    } else {
                        status.setStatus("FAIL");
                        status.setNotes("Có " + violations.size() + " vi phạm PHI");
                        status.setCheckedAt(LocalDateTime.now());
                    }
                }
                case "FIELD_LOG" -> {
                    var logs = fieldLogRepo.findBySeasonIdAndLogType(seasonId, item.getDataSourceQuery());
                    if (!logs.isEmpty()) {
                        status.setStatus("PASS");
                        status.setCheckedAt(LocalDateTime.now());
                    }
                }
            }
        }
    }

    private CertificationItemStatus findStatus(List<CertificationItemStatus> statuses, Integer itemId) {
        return statuses.stream()
            .filter(s -> s.getChecklistItemId().equals(itemId))
            .findFirst().orElse(null);
    }
}
```

### Bước 5: Frontend — Compliance Dashboard

Tạo:

```
agricultural-crop-management-frontend/src/pages/farmer/CertificationPage.tsx
```

UI components cần có:

| Component | Mô tả |
|-----------|--------|
| **Progress ring** | Hiển thị % compliance score (dùng Recharts hoặc SVG) |
| **Checklist table** | 4 nhóm VietGAP, mỗi item có status badge (PASS=green, FAIL=red, PENDING=gray) |
| **Auto-fill indicator** | Icon chỉ items được auto-fill từ dữ liệu |
| **Manual check toggle** | Farmer có thể override status |
| **Apply button** | Enable khi score ≥ 80%, gửi đơn xin chứng nhận |

```tsx
function CertificationPage() {
  const { complianceScore, isEligible } = useCertificationStatus(farmId);
  const { data: checklist } = useChecklist(farmId, standardId);

  return (
    <div>
      {/* Progress Ring */}
      <div className="flex items-center gap-4">
        <ProgressRing value={complianceScore} size={120} />
        <div>
          <Text weight="bold">Compliance Score: {complianceScore}%</Text>
          <Text>
            {complianceScore >= 80
              ? "Đủ điều kiện nộp đơn chứng nhận VietGAP"
              : `Cần thêm ${80 - complianceScore}% để đủ điều kiện`}
          </Text>
        </div>
      </div>

      {/* Checklist by category */}
      {['PRODUCTION_AREA', 'SEED', 'CULTIVATION', 'HARVEST'].map(cat => (
        <CertificationCategorySection
          key={cat}
          category={cat}
          items={checklist.filter(i => i.category === cat)}
        />
      ))}

      <Button disabled={!isEligible} className="mt-4">
        Nộp đơn xin chứng nhận VietGAP
      </Button>
    </div>
  );
}
```

### Checklist test Module 3.2

- [ ] Compliance score tự động tính đúng khi có đủ dữ liệu
- [ ] Auto-fill items từ Field Logs, Soil Tests, Water Tests hoạt động
- [ ] PHI check FAIL khi có vi phạm cách ly (test sau khi module 3.3 xong)
- [ ] "Apply" button disable khi score < 80%
- [ ] Frontend hiển thị đúng progress ring và checklist

---

## Module 3.3 — Tính toán PHI

> **Tuần 3-4** | Service: `season-service` | Database: `season_db`
>
> ⚠️ **Thứ tự triển khai:** Module 3.3 cần hoàn thành **TRƯỚC** Module 3.2 vì Certification Engine phụ thuộc vào PHI check.

### Mục tiêu

Tính toán số ngày cách ly an toàn sau khi phun thuốc BVTV. **BLOCK hoàn toàn** việc thu hoạch nếu còn trong thời gian cách ly. Đây là module nghiệp vụ quan trọng nhất — ảnh hưởng trực tiếp đến an toàn thực phẩm.

### Bước 1: Tạo Migration

```
season-service/src/main/resources/db/migration/V5__pesticide_records.sql
```

```sql
-- Bảng ghi nhận lần phun thuốc BVTV
CREATE TABLE pesticide_records (
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    season_id             INT          NOT NULL,
    plot_id               INT          NOT NULL,
    field_log_id          INT          NULL COMMENT 'Link đến field log nếu có',
    pesticide_name        VARCHAR(255)  NOT NULL COMMENT 'Tên thương mại',
    active_ingredient     VARCHAR(255)  NULL COMMENT 'Hoạt chất',
    phi_days              INT          NOT NULL COMMENT 'Số ngày cách ly theo khuyến cáo',
    harvest_allowed_date  DATE          GENERATED ALWAYS AS (application_date + INTERVAL phi_days DAY) STORED,
    application_date      DATE          NOT NULL,
    application_method    VARCHAR(100)  NULL COMMENT 'PHUN, TƯỚI, RẮC, NGÂM',
    dosage                VARCHAR(100)  NULL COMMENT 'Liều lượng đã dùng',
    target_pest           VARCHAR(255)  NULL COMMENT 'Đối tượng phòng trừ',
    note                  TEXT         NULL,
    created_by            BIGINT        NOT NULL,
    created_at            DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_pesticide_season (season_id),
    INDEX idx_pesticide_plot (plot_id),
    INDEX idx_pesticide_allowed_date (harvest_allowed_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng reference PHI của các hoạt chất phổ biến
CREATE TABLE pesticide_phi_reference (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    active_ingredient VARCHAR(255)  NOT NULL,
    pesticide_name    VARCHAR(255)  NULL COMMENT 'Tên thương mại phổ biến chứa hoạt chất này',
    phi_days          INT          NOT NULL COMMENT 'Số ngày PHI theo khuyến cáo',
    mrl_mg_per_kg     DECIMAL(10,4) NULL COMMENT 'Maximum Residue Limit',
    crop_type         VARCHAR(100)  NULL COMMENT 'Loại cây trồng áp dụng',
    source            VARCHAR(100)  DEFAULT 'EPA/CODEX' COMMENT 'Nguồn tham khảo',
    created_at        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ingredient_crop (active_ingredient, crop_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Bước 2: Seed Reference Data

```
season-service/src/main/resources/db/migration/V5__pesticide_phi_seed.sql
```

```sql
-- Seed ~30 hoạt chất phổ biến tại Việt Nam
INSERT INTO pesticide_phi_reference (active_ingredient, pesticide_name, phi_days, crop_type) VALUES
('Carbendazim', 'Carbenzim 50WP, Bavistin 50WP', 14, 'general'),
('Chlorpyrifos', 'Dursban 5G,Lorsban 150EC', 21, 'general'),
('Cypermethrin', 'Sherpa 25EC, Cypersect 25EC', 7, 'general'),
('Mancozeb', 'Manzate 80WP, Dithane M-45', 14, 'vegetable'),
('Metalaxyl', 'Ridomil Gold 68WG', 7, 'vegetable'),
('Imidacloprid', 'Confidor 100SL, Admire 50SC', 14, 'fruit'),
('Abamectin', 'Vertimec 1.8EC', 14, 'vegetable'),
('Bethecypermethrin', 'Bets 10EC', 7, 'general'),
('Fenobucarb', 'Bassa 50EC', 14, 'rice'),
('Cartap', 'Padan 95SP', 7, 'rice'),
('Carbofuran', 'Furadan 3G', 60, 'general'),
('Fenitrothion', 'Sumithion 50EC', 14, 'general'),
('Carbaryl', 'Sevin 85WP', 7, 'fruit'),
('Copper hydroxide', 'Kocide 77WP', 0, 'general'),
('Bacillus thuringiensis', 'Dipel 6.4DF', 0, 'vegetable'),
('Spinosad', 'Tracer 48SC', 1, 'vegetable'),
('Emamectin benzoate', 'Proclaim 5SG', 7, 'vegetable'),
('Hexaconazole', 'Anvil 5SC', 14, 'fruit'),
('Propiconazole', 'Tilt 250EC', 14, 'fruit'),
('Difenoconazole', 'Score 250EC', 14, 'vegetable');
```

### Bước 3: Entity + Repository

Tạo entity `PesticideRecord.java` và `PesticidePHIReference.java`.

Repository:

```java
package org.example.season.repository;

import org.example.season.entity.PesticideRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface PesticideRecordRepository extends JpaRepository<PesticideRecord, Integer> {

    List<PesticideRecord> findBySeasonId(Integer seasonId);

    // Tìm các pesticide có harvest_allowed_date trong tương lai (chưa hết cách ly)
    @Query("SELECT p FROM PesticideRecord p WHERE p.seasonId = :seasonId " +
           "AND p.harvestAllowedDate > :checkDate " +
           "ORDER BY p.harvestAllowedDate ASC")
    List<PesticideRecord> findActivePHIBySeason(
        @Param("seasonId") Integer seasonId,
        @Param("checkDate") LocalDate checkDate);

    // Tìm vi phạm PHI — harvest trước ngày cho phép
    @Query("SELECT p FROM PesticideRecord p WHERE p.seasonId = :seasonId " +
           "AND :harvestDate < p.harvestAllowedDate")
    List<PesticideRecord> findPHIViolations(
        @Param("seasonId") Integer seasonId,
        @Param("harvestDate") LocalDate harvestDate);
}
```

### Bước 4: CRITICAL — PHI Harvest Validation Service

Đây là logic **BLOCK harvest** khi vi phạm PHI:

```java
package org.example.season.service;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PHIHarvestValidationService {

    private final PesticideRecordRepository pesticideRepo;

    /**
     * Validate xem có thể thu hoạch được không.
     * @return HarvestValidationResult — isBlocked, violations list
     */
    public HarvestValidationResult validateHarvest(Integer seasonId, LocalDate harvestDate) {
        List<PesticideRecord> violations =
            pesticideRepo.findPHIViolations(seasonId, harvestDate);

        if (violations.isEmpty()) {
            return new HarvestValidationResult(false, List.of(), null);
        }

        // Tính ngày an toàn gần nhất
        LocalDate nearestSafeDate = violations.stream()
            .map(PesticideRecord::getHarvestAllowedDate)
            .max(LocalDate::compareTo)
            .orElse(harvestDate);

        List<PHIViolationDetail> details = violations.stream()
            .map(v -> new PHIViolationDetail(
                v.getPesticideName(),
                v.getActiveIngredient(),
                v.getApplicationDate(),
                v.getHarvestAllowedDate(),
                v.getPhiDays(),
                v.getHarvestAllowedDate().toEpochDay() - harvestDate.toEpochDay() // số ngày còn cách ly
            ))
            .toList();

        return new HarvestValidationResult(true, details, nearestSafeDate);
    }

    public record HarvestValidationResult(
        boolean isBlocked,
        List<PHIViolationDetail> violations,
        LocalDate nearestSafeDate
    ) {}

    public record PHIViolationDetail(
        String pesticideName,
        String activeIngredient,
        LocalDate applicationDate,
        LocalDate harvestAllowedDate,
        int phiDays,
        long daysRemaining  // số ngày còn phải chờ
    ) {}
}
```

### Bước 5: Hook vào Harvest Creation Flow

Sửa `HarvestService.java` — **thêm validation trước khi tạo harvest**:

```java
// Trong method createHarvest() hoặc updateHarvest()
public HarvestResponse createHarvest(CreateHarvestRequest req, Long userId) {
    // 1. Validate PHI trước
    HarvestValidationResult phiResult =
        phiValidationService.validateHarvest(req.seasonId(), req.harvestDate());

    if (phiResult.isBlocked()) {
        String detail = phiResult.violations().stream()
            .map(v -> v.pesticideName() + " (cách ly đến " + v.harvestAllowedDate() + ")")
            .collect(Collectors.joining(", "));

        throw new IllegalStateException(
            "THU HOẠCH BỊ CHẶN: Còn " + phiResult.violations().size() +
            " thuốc BVTV chưa hết thời gian cách ly. " +
            "Danh sách: " + detail + ". " +
            "Ngày thu hoạch an toàn gần nhất: " + phiResult.nearestSafeDate());
    }

    // 2. Tiếp tục tạo harvest bình thường
    Harvest harvest = Harvest.builder()
        .seasonId(req.seasonId())
        .harvestDate(req.harvestDate())
        .quantity(req.quantity())
        // ...
        .build();

    return toResponse(repository.save(harvest));
}
```

### Bước 6: Auto-lookup PHI từ Reference

Khi farmer ghi Field Log loại `PESTICIDE_APPLICATION` → auto-fill `phi_days`:

```java
public PesticideRecord createFromFieldLog(Integer fieldLogId, Long userId) {
    FieldLog log = fieldLogRepo.findById(fieldLogId)
        .orElseThrow(() -> new ResourceNotFoundException("Field log not found"));

    // Parse thuốc từ notes hoặc dedicated field
    String pesticideName = extractPesticideName(log.getNotes());

    // Lookup PHI từ reference table
    Integer phiDays = pesticidePhiReferenceRepo
        .findByPesticideNameContainingIgnoreCase(pesticideName)
        .map(PesticidePHIReference::getPhiDays)
        .orElseThrow(() -> new BadRequestException(
            "Không tìm thấy thông tin PHI cho thuốc: " + pesticideName +
            ". Vui lòng nhập số ngày cách ly thủ công."));

    PesticideRecord record = PesticideRecord.builder()
        .seasonId(log.getSeason().getId())
        .plotId(log.getSeason().getPlotId())
        .fieldLogId(fieldLogId)
        .pesticideName(pesticideName)
        .applicationDate(log.getLogDate())
        .phiDays(phiDays)
        .createdBy(userId)
        .build();

    return repository.save(record);
}
```

### Bước 7: Frontend — Harvest Safety Widget

Thêm widget vào **Season Dashboard** (trang `SeasonsPage.tsx`):

```tsx
// Widget: Harvest Safety Status
function HarvestSafetyStatus({ seasonId, seasonStatus }) {
  const { data: activePHI } = useQuery({
    queryKey: ['season-phi', seasonId],
    queryFn: () => api.get(`/seasons/${seasonId}/phi/active`),
  });

  const today = new Date().toISOString().slice(0, 10);
  const hasActivePHI = activePHI && activePHI.length > 0;
  const nearestSafe = activePHI?.[0]?.harvestAllowedDate;

  return (
    <div className={cn(
      "p-4 rounded-xl border",
      !hasActivePHI && "border-emerald-200 bg-emerald-50",
      hasActivePHI && "border-amber-200 bg-amber-50"
    )}>
      {!hasActivePHI ? (
        <div className="flex items-center gap-2 text-emerald-800">
          <CheckCircle className="w-5 h-5" />
          <Text weight="semibold">Đã hết thời gian cách ly — Có thể thu hoạch</Text>
        </div>
      ) : (
        <div className="space-y-2">
          <div className="flex items-center gap-2 text-amber-800">
            <AlertTriangle className="w-5 h-5" />
            <Text weight="semibold">
              {activePHI.length} thuốc BVTV đang trong thời gian cách ly
            </Text>
          </div>
          <div className="text-sm text-amber-700">
            Thuốc gần nhất hết cách ly: <strong>{nearestSafe}</strong>
          </div>
        </div>
      )}
    </div>
  );
}
```

Thêm PILL badge màu trên nút "Tạo thu hoạch" trong UI:

- 🟢 Xanh: `isBlocked = false` → bình thường
- 🟡 Vàng: `isBlocked = true` nhưng `daysRemaining ≤ 3` → sắp hết cách ly
- 🔴 Đỏ: `isBlocked = true` và `daysRemaining > 3` → đang trong cách ly

### Checklist test Module 3.3

- [x] `POST /seasons/1/phi/records` → tạo pesticide record với auto-calculated harvest_allowed_date
- [x] `POST /seasons/1/harvests` với harvest_date < harvest_allowed_date → **HTTP 400** với message rõ ràng
- [x] `POST /seasons/1/harvests` với harvest_date > harvest_allowed_date → **201 Created**
- [x] Lookup PHI từ reference table khi nhập tên thuốc Carbendazim → auto-fill 14 ngày
- [x] Frontend: widget hiển thị đúng màu (xanh/vàng/đỏ) theo trạng thái PHI

---

## Module 3.4 — Minh bạch Người tiêu dùng

> **Tuần 8-10** | Service: `marketplace-service` | Database: `marketplace_db`

### Mục tiêu

Người mua quét QR code → trang `/trace/{productId}` công khai (không cần đăng nhập) hiển thị toàn bộ hành trình sản phẩm kèm certification badge và PHI safety status.

### Bước 1: Mở rộng MarketplaceTraceabilityResponse

Sửa `MarketplaceTraceabilityResponse.java`:

```java
// Thêm vào record chính:
public record MarketplaceTraceabilityResponse(
    // ... existing fields ...

    // MỚI: Certification info
    CertificationInfo certification,

    // MỚI: PHI summary
    PHISafetyInfo phiSafety,

    // MỚI: Nutrition info từ soil tests
    NutritionClaim nutritionClaim,
) {
    // Inner records mới
    public record CertificationInfo(
        String certificationName,   // "VietGAP Trồng trọt 2024"
        String certificationType,   // "VIETGAP_PLANTING"
        String status,              // "ACTIVE", "PENDING", "EXPIRED"
        LocalDate issuedDate,
        LocalDate expiryDate,
        BigDecimal complianceScore  // % compliance khi apply
    ) {}

    public record PHISafetyInfo(
        boolean isSafe,             // true nếu không có vi phạm PHI
        int totalPesticidesUsed,
        int safePesticides,         // đã hết cách ly
        int cautionPesticides,     // sắp hết cách ly (≤3 ngày)
        List<PesticideUsageItem> usage
    ) {
        public record PesticideUsageItem(
            String pesticideName,
            LocalDate applicationDate,
            LocalDate harvestAllowedDate,
            String status  // SAFE, CAUTION, BLOCKED
        ) {}
    }

    public record NutritionClaim(
        String soilOrganicMatter,
        String soilPH,
        String nitrogenLevel
    ) {}
}
```

### Bước 2: Tạo Public Traceability Endpoint

Sửa `MarketplaceController.java` — thêm endpoint công khai:

```java
@GetMapping("/products/{productId}/public-trace")
public ResponseEntity<MarketplaceTraceabilityResponse> getPublicTrace(
        @PathVariable Long productId) {
    // Không cần @AuthenticationPrincipal — endpoint public
    return ResponseEntity.ok(marketplaceService.getPublicTraceability(productId));
}
```

Trong `MarketplaceServiceImpl`:

```java
public MarketplaceTraceabilityResponse getPublicTraceability(Long productId) {
    // Gọi buildTraceabilityResponse (đã có) để lấy base data
    MarketplaceTraceabilityResponse base = buildTraceabilityResponse(product);

    // Bổ sung certification info (gọi farm-service qua Feign)
    CertificationInfo certInfo = fetchCertificationInfo(product.getFarmId());

    // Bổ sung PHI safety (gọi season-service)
    PHISafetyInfo phiInfo = fetchPHISafety(product.getSeasonId());

    return new MarketplaceTraceabilityResponse(
        base.productId(), base.traceable(),
        base.farm(), base.plot(), base.season(),
        base.harvest(), base.productLot(), base.timeline(),
        base.validatedAt(),
        certInfo,   // ← MỚI
        phiInfo,    // ← MỚI
        null        // nutritionClaim (phase sau)
    );
}

// NOTE: Không expose bất kỳ internal cost, notes, personal farmer data nào
```

### Bước 3: Cấu hình Security — Public Route

Trong `application.yml` của `marketplace-service`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # ... existing config ...
          # Cho phép public access đến public-trace
```

Hoặc trong Spring Security config:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/api/v1/marketplace/products/*/public-trace",
                "/api/v1/marketplace/products/{id}/traceability",
                "/api/v1/marketplace/products/**",
                "/api/v1/marketplace/farms/**"
            ).permitAll()
            // ... các route khác authenticated
        );
    return http.build();
}
```

### Bước 4: QR Code Generation

Tạo utility:

```java
// marketplace-service/src/main/java/.../util/QRCodeGenerator.java
@Component
public class QRCodeGenerator {

    @Value("${app.public.trace.base-url:https://acm.vietfuture.vn}")
    private String baseUrl;

    public String generateTraceUrl(Long productId, String slug) {
        return baseUrl + "/trace/" + (slug != null ? slug : productId);
    }

    // Generate QR code as byte[] (PNG) — dùng library 'zxing'
    public byte[] generateQRImage(String content, int width) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix matrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, width);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }
}
```

### Bước 5: Public Trace Page (Frontend)

Tạo route và page:

```
agricultural-crop-management-frontend/src/pages/public/PublicTracePage.tsx
```

```tsx
// Route: /trace/[slug]
export function PublicTracePage() {
  const { slug } = useParams();
  const { data: trace, isLoading } = usePublicTraceability(slug);

  if (isLoading) return <TracePageSkeleton />;
  if (!trace) return <NotFoundPage />;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-emerald-700 text-white p-4">
        <Text weight="bold" className="text-xl">Truy xuất nguồn gốc</Text>
        <Text>{trace.farm?.name} — {trace.productId}</Text>
      </div>

      {/* Certification Badge */}
      {trace.certification && (
        <div className={cn(
          "p-4 flex items-center gap-3",
          trace.certification.status === 'ACTIVE'
            ? "bg-emerald-50 border-emerald-200"
            : "bg-gray-50 border-gray-200"
        )}>
          <Badge variant={trace.certification.status === 'ACTIVE' ? 'success' : 'outline'}>
            {trace.certification.certificationName}
          </Badge>
          {trace.certification.status === 'ACTIVE' && (
            <Text size="small">
              Hạn chứng nhận: {trace.certification.expiryDate}
            </Text>
          )}
        </div>
      )}

      {/* PHI Safety Status */}
      {trace.phiSafety && (
        <div className={cn(
          "p-4 border-b",
          trace.phiSafety.isSafe ? "bg-emerald-50" : "bg-amber-50"
        )}>
          <div className="flex items-center gap-2">
            {trace.phiSafety.isSafe
              ? <CheckCircle className="text-emerald-600" />
              : <AlertTriangle className="text-amber-600" />}
            <Text weight="semibold">
              {trace.phiSafety.isSafe
                ? "An toàn — Đã hết thời gian cách ly"
                : `Cảnh báo — ${trace.phiSafety.cautionPesticides} thuốc chưa hết cách ly`}
            </Text>
          </div>
        </div>
      )}

      {/* Timeline */}
      <TraceTimeline milestones={trace.timeline} />

      {/* Farm & Season Info */}
      <TraceSection title="Thông tin nông trại">
        <TraceRow label="Tên" value={trace.farm?.name} />
        <TraceRow label="Vùng" value={trace.farm?.region} />
        <TraceRow label="Mùa vụ" value={trace.season?.name} />
        <TraceRow label="Cây trồng" value={trace.season?.cropName} />
      </TraceSection>

      {/* Harvest Info */}
      {trace.harvest && (
        <TraceSection title="Thu hoạch">
          <TraceRow label="Ngày thu hoạch" value={trace.harvest.harvestDate} />
          <TraceRow label="Số lượng" value={`${trace.harvest.quantity} kg`} />
          <TraceRow label="Phẩm chất" value={trace.harvest.qualityNotes} />
        </TraceSection>
      )}
    </div>
  );
}
```

### Checklist test Module 3.4

- [ ] `GET /products/1/public-trace` → **200** (không cần auth token)
- [ ] Response chứa `certification` và `phiSafety`
- [ ] QR code generation → file PNG hợp lệ
- [ ] Public Trace Page responsive trên mobile
- [ ] Không có internal cost/notes/personal data trong response public

---

## Module 4.1 — Tối ưu UI/UX

> **Tuần 11-12** | Service: `agricultural-crop-management-frontend`

### Bước 1: Refactor Farmer Dashboard

Tìm file dashboard hiện tại, refactor để KPIs nằm trên cùng:

```
agricultural-crop-management-frontend/src/pages/farmer/FarmerDashboardPage.tsx
```

```tsx
// Trước: KPIs buried in middle of page
// Sau: KPIs as first card row

function FarmerDashboardPage() {
  return (
    <div className="space-y-6">
      {/* Row 1: KPIs — nằm ngay đầu trang */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <KPICard
          label="Sản lượng mùa vụ"
          value={`${currentSeasonYield} kg`}
          trend="+12%"
          icon={<TrendingUp />}
        />
        <KPICard
          label="Chi phí mùa vụ"
          value={formatVnd(totalExpense)}
          trend="-5%"
          icon={<DollarSign />}
        />
        <KPICard
          label="Đơn hàng chờ"
          value={pendingOrders}
          icon={<ShoppingCart />}
          highlight={pendingOrders > 0}
        />
        <KPICard
          label="Compliance Score"
          value={`${complianceScore}%`}
          icon={<Shield />}
          tone={complianceScore >= 80 ? 'success' : 'warning'}
        />
      </div>

      {/* Row 2: Quick Actions */}
      <QuickActionsPanel
        actions={[
          { label: 'Ghi nhật ký', icon: <Edit />, href: '/farmer/field-logs' },
          { label: 'Tạo thu hoạch', icon: <Package />, href: '/farmer/harvests' },
          { label: 'Xem đơn hàng', icon: <ShoppingBag />, href: '/farmer/orders' },
          { label: 'Tài liệu nông trại', icon: <FileText />, href: '/farmer/farm-documents' },
        ]}
      />

      {/* Row 3: Charts inline */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <SeasonYieldChart seasonId={currentSeasonId} />
        <ExpenseBreakdownChart seasonId={currentSeasonId} />
      </div>
    </div>
  );
}
```

### Bước 2: Tạo SearchWindow Component

```tsx
// agricultural-crop-management-frontend/src/shared/components/SearchWindow.tsx
export function SearchWindow({ open, onOpenChange, onSelect }) {
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState([]);

  // Debounce 300ms
  useEffect(() => {
    if (query.length < 2) { setResults([]); return; }
    const timer = setTimeout(async () => {
      setLoading(true);
      const data = await searchAll(query); // search tasks, products, docs
      setResults(data);
      setLoading(false);
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="p-0 overflow-hidden">
        <div className="p-4 border-b">
          <Input
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder="Tìm kiếm nhanh..."
            autoFocus
          />
        </div>
        <div className="max-h-96 overflow-y-auto">
          {loading && <div className="p-4 text-center"><Loader /></div>}
          {results.map(r => (
            <SearchResultItem key={r.id} result={r} onSelect={() => {
              onSelect(r);
              onOpenChange(false);
            }} />
          ))}
        </div>
      </DialogContent>
    </Dialog>
  );
}

// Thêm global keyboard shortcut
// Trong App component hoặc root layout:
useEffect(() => {
  const handler = (e) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
      e.preventDefault();
      setSearchOpen(true);
    }
  };
  window.addEventListener('keydown', handler);
  return () => window.removeEventListener('keydown', handler);
}, []);
```

### Checklist test Module 4.1

- [ ] KPIs hiển thị ngay đầu dashboard, không cần scroll
- [ ] Cmd+K mở SearchWindow
- [ ] ESC đóng SearchWindow
- [ ] Debounced search không gây lag
- [ ] Responsive trên mobile

---

## Module 4.2 — Tích hợp Giao hàng

> **Tuần 13-16** | Service: **Delivery Service MỚI** | Database: **delivery_db mới**

### Mục tiêu

Tính phí ship động, hỗ trợ giao hàng nhanh cho nông sản tươi, cold chain.

### Bước 1: Tạo Spring Boot Service mới

Tạo thư mục mới:

```
delivery-service/
├── src/main/java/org/example/delivery/
│   ├── DeliveryApplication.java
│   ├── entity/
│   │   ├── DeliveryOrder.java
│   │   ├── DeliveryProvider.java
│   │   ├── DeliveryRate.java
│   │   └── enums/DeliveryStatus.java
│   ├── dto/
│   │   ├── request/CalculateShippingRequest.java
│   │   └── response/ShippingOption.java
│   ├── repository/
│   ├── service/
│   │   ├── ShippingFeeCalculator.java
│   │   └── DeliveryService.java
│   └── controller/
│       └── DeliveryController.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/V8__delivery.sql
├── pom.xml
└── Dockerfile
```

### Bước 2: Database Migration

```
delivery-service/src/main/resources/db/migration/V8__delivery.sql
```

```sql
CREATE TABLE delivery_providers (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(20)  NOT NULL UNIQUE COMMENT 'GHTK, GHN, NINJA_VAN, JT_EXPRESS',
    name          VARCHAR(100)  NOT NULL,
    supports_cold_chain BOOLEAN DEFAULT FALSE,
    supports_same_day    BOOLEAN DEFAULT FALSE,
    is_active     BOOLEAN     DEFAULT TRUE,
    api_endpoint  VARCHAR(500) NULL COMMENT 'Real API endpoint (null = demo mode)',
    api_key       VARCHAR(255) NULL,
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE delivery_rates (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    provider_id    INT          NOT NULL,
    zone_from      VARCHAR(50)  NOT NULL COMMENT 'Tỉnh/thành gửi',
    zone_to         VARCHAR(50)  NOT NULL COMMENT 'Tỉnh/thành nhận',
    weight_min_kg   DECIMAL(10,2) NOT NULL DEFAULT 0,
    weight_max_kg   DECIMAL(10,2) NOT NULL,
    base_rate_vnd   DECIMAL(12,2) NOT NULL COMMENT 'Phí cơ bản (VNĐ)',
    per_kg_vnd     DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Phí theo kg vượt',
    estimated_hours INT         DEFAULT 48 COMMENT 'Ước tính giao (giờ)',
    is_cold_chain   BOOLEAN     DEFAULT FALSE,
    cold_chain_fee_vnd DECIMAL(12,2) DEFAULT 0,

    INDEX idx_rates_provider (provider_id),
    INDEX idx_rates_zone (zone_from, zone_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE delivery_orders (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    marketplace_order_id BIGINT      NOT NULL COMMENT 'Link đến MarketplaceOrder',
    provider_id          INT         NOT NULL,
    tracking_number      VARCHAR(100) NULL,
    status               VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, PICKUP_SCHEDULED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, RETURNED, CANCELLED',
    shipping_fee_vnd     DECIMAL(12,2) NOT NULL,
    estimated_delivery    DATETIME    NULL,
    actual_delivery      DATETIME    NULL,
    is_perishable        BOOLEAN     DEFAULT FALSE,
    requires_cold_chain   BOOLEAN     DEFAULT FALSE,
    recipient_name       VARCHAR(255) NOT NULL,
    recipient_phone      VARCHAR(20)  NOT NULL,
    recipient_address    VARCHAR(500) NOT NULL,
    recipient_province    VARCHAR(100) NOT NULL,
    weight_kg            DECIMAL(10,2) DEFAULT 0,
    created_at            DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_delivery_marketplace (marketplace_order_id),
    INDEX idx_delivery_status (status),
    INDEX idx_delivery_tracking (tracking_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed demo rates
INSERT INTO delivery_providers (code, name, supports_cold_chain, supports_same_day) VALUES
('GHTK', 'Giao hàng tiết kiệm', TRUE, TRUE),
('GHN', 'Giao hàng nhanh', TRUE, TRUE),
('NINJA_VAN', 'Ninja Van', FALSE, FALSE),
('JT_EXPRESS', 'J&T Express', FALSE, FALSE);
```

### Bước 3: Shipping Fee Calculator

```java
@Service
@RequiredArgsConstructor
public class ShippingFeeCalculator {

    /**
     * Tính phí ship cho 1 đơn hàng.
     * Algorithm: base_rate + (weight - weight_min) × per_kg_rate
     * Cold chain: +cold_chain_fee nếu requires_cold_chain = true
     */
    public List<ShippingOption> calculateOptions(CalculateShippingRequest req) {
        List<DeliveryProvider> providers = providerRepo.findActiveProviders();

        return providers.stream()
            .filter(p -> {
                if (req.requiresColdChain() && !p.getSupportsColdChain()) return false;
                if (req.prefersSameDay() && !p.getSupportsSameDay()) return false;
                return true;
            })
            .flatMap(p -> buildOptionsForProvider(p, req).stream())
            .sorted(Comparator.comparing(ShippingOption::shippingFeeVnd))
            .toList();
    }

    private List<ShippingOption> buildOptionsForProvider(
            DeliveryProvider provider, CalculateShippingRequest req) {
        List<ShippingOption> options = new ArrayList<>();

        // Standard option
        DeliveryRate standardRate = findRate(provider.getId(),
            req.senderProvince(), req.recipientProvince(),
            req.weightKg(), false);

        if (standardRate != null) {
            long fee = calculateFee(standardRate, req.weightKg(), false);
            options.add(new ShippingOption(
                "standard", provider.getId(), provider.getName(),
                BigDecimal.valueOf(fee),
                standardRate.getEstimatedHours(),
                false,
                provider.getSupportsSameDay()
            ));
        }

        // Same-day option (nếu hỗ trợ và trong cùng thành phố)
        if (provider.getSupportsSameDay() &&
            req.senderProvince().equals(req.recipientProvince())) {
            DeliveryRate sameDayRate = findRate(provider.getId(),
                req.senderProvince(), req.recipientProvince(),
                req.weightKg(), true);

            if (sameDayRate != null) {
                long fee = calculateFee(sameDayRate, req.weightKg(), true);
                options.add(new ShippingOption(
                    "same_day", provider.getId(), provider.getName(),
                    BigDecimal.valueOf(fee),
                    4,  // giao trong 4 giờ
                    true, false
                ));
            }
        }

        return options;
    }

    private long calculateFee(DeliveryRate rate, BigDecimal weightKg, boolean isColdChain) {
        BigDecimal baseFee = rate.getBaseRateVnd();
        BigDecimal extraKg = weightKg.subtract(rate.getWeightMinKg());
        if (extraKg.compareTo(BigDecimal.ZERO) < 0) extraKg = BigDecimal.ZERO;

        BigDecimal weightFee = extraKg.multiply(rate.getPerKgVnd());
        BigDecimal coldChainFee = isColdChain ? rate.getColdChainFeeVnd() : BigDecimal.ZERO;

        return baseFee.add(weightFee).add(coldChainFee).longValue();
    }

    public record ShippingOption(
        String type,          // "standard" | "same_day"
        Integer providerId,
        String providerName,
        BigDecimal shippingFeeVnd,
        int estimatedHours,
        boolean isSameDay,
        boolean isColdChain
    ) {}
}
```

### Bước 4: Thêm vào Marketplace Checkout Flow

Trong checkout page của frontend, thêm bước **chọn vận chuyển**:

```tsx
function CheckoutPage() {
  const { cart } = useCart();

  // Tính shipping options khi có địa chỉ
  const { data: shippingOptions } = useQuery({
    queryKey: ['shipping-options', cart.totalWeight, recipientAddress],
    queryFn: () => deliveryApi.calculate({
      senderProvince: sellerProvince,
      recipientProvince: recipientAddress.province,
      weightKg: cart.totalWeight,
      requiresColdChain: cart.hasPerishableItems,
      prefersSameDay: cart.prefersSameDay,
    }),
    enabled: !!recipientAddress,
  });

  const [selectedOption, setSelectedOption] = useState(null);

  return (
    <div>
      <ShippingOptionsList
        options={shippingOptions}
        selected={selectedOption}
        onSelect={setSelectedOption}
      />

      {selectedOption && (
        <div className="mt-4 p-4 bg-emerald-50 rounded-xl">
          <Text weight="semibold">
            Phí vận chuyển: {formatVnd(selectedOption.shippingFeeVnd)}
          </Text>
          <Text size="small">
            Ước tính giao: {selectedOption.estimatedHours}h
            {selectedOption.isSameDay && ' — Giao trong ngày'}
          </Text>
        </div>
      )}
    </div>
  );
}
```

### Checklist test Module 4.2

- [ ] `POST /api/v1/delivery/calculate` → danh sách shipping options
- [ ] Phí tính đúng: base + per_kg + cold_chain (nếu có)
- [ ] Filter provider theo cold chain support
- [ ] Checkout flow hiển thị shipping options
- [ ] Farmer portal hiển thị delivery orders tab

---

## Checklist trước khi deploy

### Backend
- [ ] Tất cả Flyway migrations chạy thành công (V4 → V8)
- [ ] Các endpoint mới trả về đúng HTTP status codes
- [ ] Feature flags off by default, bật từng module
- [ ] No breaking changes với API contracts cũ
- [ ] RabbitMQ events được publish/consume đúng

### Frontend
- [ ] Tất cả route mới hoạt động
- [ ] Navigation menus được cập nhật
- [ ] Responsive trên mobile
- [ ] No TypeScript errors
- [ ] Loading states và error states cho tất cả API calls

### Integration
- [ ] Farm Document → Certification flow hoạt động
- [ ] PHI validation → BLOCK harvest hoạt động
- [ ] Certification → Public Trace hiển thị compliance badge
- [ ] Delivery calculation → Checkout flow hoàn chỉnh
- [ ] Message queue events giữa services đúng thứ tự

### Rollback plan
```bash
# Nếu cần rollback module nào đó:
flyway -url=jdbc:mysql://localhost:3306/farm_db -user=root -password=... migrate:undo -target=V3
flyway -url=jdbc:mysql://localhost:3306/season_db migrate:undo -target=V4
flyway -url=jdbc:mysql://localhost:3306/marketplace_db migrate:undo -target=V6
# Delivery service: docker-compose down && xóa volume delivery_db
```

---

## Thứ tự triển khai tóm tắt

| Thứ tự | Module | Lý do |
|---------|--------|--------|
| **1** | 3.1 Farm Documents | Ít rủi ro nhất, nền tảng cho 3.2 |
| **2** | 3.3 PHI Calculator | Phụ thuộc bởi 3.2 và 3.4 |
| **3** | 3.2 Certification Engine | Phụ thuộc 3.1 và 3.3 |
| **4** | 3.4 Public Traceability | Mở rộng 3.2 và 3.3 |
| **5** | 4.1 UI/UX Optimization | Không phụ thuộc backend |
| **6** | 4.2 Delivery Integration | Microservice mới, độc lập |

---

> Hướng dẫn này được tạo dựa trên phân tích chi tiết codebase VietFuture2026 và các tiêu chuẩn nông nghiệp Việt Nam (VietGAP TCVN 11892-1:2017, QCVN 08-MT:2015/BTNMT).
