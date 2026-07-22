# BÁO CÁO KIỂM TRA LẠI — Commit "VietGAP Buoc 1"
## Đối chiếu với BRD_VietFuture2026_Farmer_VietGAP.md

| | |
|---|---|
| **Commit kiểm tra** | `8d8382d3` — "VietGAP Buoc 1" (2026-07-19 14:56:35 +0700) |
| **Phạm vi** | 94 file thay đổi trên 5 service: `farm-service`, `season-service`, `marketplace-service`, `delivery-service`, `crop-catalog-service` |
| **Phương pháp** | Clone trực tiếp `main`, đọc byte-level từng file thay đổi (`file`, `wc -l`, `od -c`), đối chiếu logic với BRD |

---

## ⚠️ KẾT LUẬN CHÍNH: Chưa thể coi là "hoàn thành lộ trình Backend"

**Bằng chứng cụ thể, không suy đoán:** trong 94 file bị thay đổi ở commit này, **61 file (~65%) bị hỏng ở mức byte** — toàn bộ nội dung file (hoặc phần mới được chèn vào) chứa **chuỗi ký tự literal `\r\n`** (dấu gạch chéo ngược + chữ "r" + gạch chéo ngược + chữ "n") thay vì **ký tự xuống dòng thật**. Hệ quả: **cả 5 service bị chạm tới trong commit này đều KHÔNG BUILD ĐƯỢC** ở trạng thái hiện tại.

### Bằng chứng byte-level (không thể nguỵ biện là do cách hiển thị terminal)

```
$ file farm-service/.../enums/AuditStatus.java
AuditStatus.java: ASCII text, with very long lines (1788)   ← cả file chỉ có 1 dòng vật lý

$ wc -l AuditStatus.java
1   AuditStatus.java                                        ← xác nhận: không có ký tự \n thật nào

$ od -c AuditStatus.java | head -3
p  a  c  k  a  g  e     o  r  g  .  e  x  a  m
p  l  e  .  f  a  r  m  .  e  n  u  m  s  ;  \
r  \  n  \  r  \  n  p  u  b  l  i  c     e  n   ← "\r\n" là 4 KÝ TỰ VĂN BẢN, không phải byte CR/LF
u  m     A  u  d  i  t  S  t  a  t  u  s     {
```

Trong Java, dấu `\` đứng ngoài string/char literal (và không phải `\uXXXX`) là **ký tự bất hợp pháp** → trình biên dịch sẽ báo lỗi cú pháp ngay tại vị trí này. Điều này đúng với **mọi** file bị dính lỗi, dù đó là entity, DTO, controller, repository hay file `.sql` migration.

### Phạm vi ảnh hưởng theo service

| Service | Số file bị hỏng / tổng số file đổi | Hệ quả |
|---|---|---|
| `farm-service` | 29 file | Toàn bộ backbone của Luồng D (Audit, Nonconformity, Corrective Action) — entity, repository, controller đều hỏng → **service không build** |
| `season-service` | 20 file | Employee Training + Production Diary + PHI Alert — entity, controller, service đều hỏng → **service không build** |
| `marketplace-service` | 6 file | Bao gồm `MarketplaceService.java` (interface) và phần mới chèn vào `MarketplaceServiceImpl.java` (dòng 1789+) → **service không build** |
| `delivery-service` | 3 file | `BatchSuggestionResponse.java` + 2 migration → **service không build** (dù `DeliveryController`/`DeliveryService` sửa sạch, chúng gọi thẳng tới DTO bị hỏng) |
| `crop-catalog-service` | 3 file | Migration `V4__add_storage_profile_to_crop.sql` hỏng → **Flyway sẽ fail ngay khi khởi động, service không lên được** |

**Kết luận kỹ thuật:** vì Maven biên dịch toàn bộ source tree của 1 module trong 1 lần, chỉ cần 1 file `.java` lỗi cú pháp là **toàn bộ module đó build fail** — không có khái niệm "phần nào chạy được, phần nào không" ở cấp module. Nói cách khác: **hiện tại không service nào trong 5 service trên có thể chạy**, kể cả `season-service` vốn trước đó chỉ vướng 1 bug nhỏ (đã được fix đúng, xem bên dưới).

### Migration SQL còn nghiêm trọng hơn Java

Với các file `.sql`, lỗi này không chỉ là "khó đọc" — nó **thay đổi ngữ nghĩa thực thi**:
```sql
-- =====================...\r\n-- V8: Certification Audit Workflow\r\n...\r\nCREATE TABLE certification_audits (...
```
Vì `--` trong SQL là comment **chạy tới hết dòng vật lý**, và file này **không có dòng vật lý thứ 2** (toàn bộ là 1 dòng), nên về mặt lý thuyết **toàn bộ nội dung sau dấu `--` đầu tiên — bao gồm tất cả các câu lệnh `CREATE TABLE`, `ALTER TABLE` — bị nuốt vào trong comment và không bao giờ thực thi**. Với các migration không mở đầu bằng `--` (như `V17__add_employee_training.sql`, bắt đầu thẳng bằng `CREATE TABLE`), dấu `\` xuất hiện giữa các token vẫn khiến MySQL báo lỗi cú pháp ngay khi Flyway chạy. **Cả hai trường hợp đều dẫn tới: migration fail → Flyway chặn service khởi động** (hành vi mặc định của Flyway khi 1 migration fail).

---

## ✅ Tin tốt: Phần LOGIC nghiệp vụ (ở những file không bị hỏng) bám khá sát BRD

Cần công bằng: đây **không phải** vấn đề về tư duy thiết kế — nhiều file chứa logic cốt lõi lại **hoàn toàn sạch, biên dịch được, và đúng theo đặc tả**:

- `CertificationAuditService.java` (farm-service) — **sạch**. State machine `VALID_TRANSITIONS` implement đúng §5.4 (kể cả nhánh `REJECTED → IN_PROGRESS`), có guard `BR-D-01` (chặn issue khi còn nonconformity `CRITICAL` đang `OPEN`), có auto-publish khi Admin verify document loại `CERTIFICATE` đúng `BR-D-02`.
- `CertificationService.java`, `ErrorCode.java`, `CertificationScoringService.java` (farm-service) — **sạch**, mã lỗi mới (`CERTIFICATION_INVALID_TRANSITION`, `CRITICAL_NONCONFORMITY_OPEN`...) đã thêm đúng.
- `MarketplaceComplianceGateService.java` (marketplace-service, 117 dòng) — **sạch**. Implement đúng `BR-G-01` (bỏ qua nếu `complianceClaim = NONE`), đúng `BR-G-02` (lưu snapshot khi PASS), có check hết hạn chứng nhận, có check PHI qua `SeasonClient`, có trả `missingMandatoryEvidenceCount`/`missingEvidenceItems` đúng như thiết kế "tái sử dụng, không xây điểm số song song" ở §8.4.
- `Crop.java` (crop-catalog-service) — **sạch**, đã thêm đúng field `shelfLifeDays`, `defaultStorageCategory`, `requiresColdChain` theo §4.6.
- **Bug P0 gốc (`Task.java` khai báo trùng `plotName`) đã được fix đúng** — diff sạch, có `import java.math.BigDecimal`, không còn field trùng.

→ Điều này cho thấy đội ngũ/AI thực hiện **hiểu đúng và bám sát BRD**, vấn đề nằm ở **khâu ghi file** (rất có thể là lỗi trong pipeline/tool áp dụng patch — nơi nào đó chuỗi `"\r\n"` bị serialize thành text thay vì được unescape thành byte CRLF thật trước khi ghi đĩa), không phải ở chất lượng thiết kế.

---

## 🔴 Đối chiếu lại roadmap P0–P8 theo hiện trạng THẬT

| Giai đoạn | Nội dung | Trạng thái thực tế sau audit |
|---|---|---|
| P0 | Fix bug `Task.java` | ✅ **Đã fix đúng, sạch** |
| P1 | Luồng D — Certification Audit Lifecycle | 🔴 **Logic đã viết đúng nhưng 29/34 file liên quan bị hỏng byte → farm-service không build** |
| P2 | Luồng G — Marketplace Compliance Gate | 🔴 **Logic lõi (`MarketplaceComplianceGateService`) sạch và đúng, nhưng phụ thuộc `ComplianceCheckResponse` + phần vá vào `MarketplaceServiceImpl`/`MarketplaceService` bị hỏng → marketplace-service không build** |
| P3 | PHI widget + Crop storage profile | 🟡 **`PHIAlertService`/`PHIAlertDto`/`DashboardController` (season) bị hỏng; migration crop storage profile bị hỏng VÀ thiếu cột `shelf_life_days` so với entity (gap ngay cả khi hết hỏng)** |
| P4 | Luồng E — Production Diary + AI hook | 🔴 **`ProductionDiaryAggregationService`, `ProductionDiaryController`, `SustainabilityServiceClient` đều bị hỏng → không build. Chưa thấy phần "tạo PesticideRecord từ gợi ý AI" (§6.5 bước 5) được implement** |
| P5 | Luồng K — Employee Training | 🔴 **Toàn bộ entity/repository/service/controller bị hỏng → không build. Chưa thấy phần liên kết vào `CertificationScoringService` (§11.4)** |
| P6 | Luồng I — Pre-order + Batching | 🟡 **Migration + DTO batching bị hỏng; chưa thấy field `isPreOrder`/`requestedDeliveryDate` được dùng ở đâu trong `MarketplaceServiceImpl` (chỉ mới có cột DB dự kiến qua migration hỏng, chưa có luồng checkout thực tế)** |
| P7 | QA Frontend (Luồng A/B/C) | ⚪ Không đổi — đúng như bạn nói, độc lập, có thể làm riêng |
| **P8** | **Luồng H — đối chiếu snapshot vào trang truy xuất công khai** | 🔴 **CHƯA LÀM.** `MarketplaceTraceabilityResponse.java` và toàn bộ code truy xuất công khai **không xuất hiện trong commit này** (0 thay đổi). Snapshot (`certificationSnapshotJson`/`harvestSafetySnapshotJson`) đã được *lưu* khi duyệt sản phẩm, nhưng **chưa được đọc ra** ở trang trace công khai như BRD §9.3 yêu cầu |

→ **P8 — mục bạn nói là "mục triển khai kỹ thuật Backend cuối cùng" — thực tế chưa được động tới.**

### Về Frontend cho các luồng MỚI (khác với P7)
Cần phân biệt rõ: P7 trong BRD chỉ nói về **QA giao diện cho luồng CŨ đã có sẵn** (A/B/C). Còn giao diện cho **toàn bộ tính năng MỚI** (quản lý Audit chứng nhận, Nonconformity, Corrective Action, Đào tạo nhân viên, Nhật ký sản xuất hợp nhất, Compliance Gate cho Admin, Pre-order...) — audit xác nhận **commit này có 0 thay đổi ở `agricultural-crop-management-frontend/`**. Tức là kể cả khi backend build được, hiện **chưa có bất kỳ màn hình nào** để Farmer/Auditor/Admin thao tác với các tính năng mới này.

---

## Việc cần làm ngay (ưu tiên theo đúng thứ tự chặn)

1. **[P0 mới, chặn tuyệt đối]** Chuẩn hoá lại line-ending cho 61 file trong danh sách ở Phụ lục — chuyển `\r\n` (literal text) thành ký tự xuống dòng thật. Cách nhanh nhất: viết 1 script (Python/Node) đọc từng file, `content.replace('\\r\\n', '\n')` (thay chuỗi literal, **không phải** regex xử lý byte CRLF), ghi đè lại bằng chế độ text UTF-8. Sau đó chạy `mvn -pl farm-service,season-service,marketplace-service,delivery-service,crop-catalog-service compile` để xác nhận build xanh.
2. Sau khi build được, chạy lại Flyway (`mvn flyway:migrate` hoặc khởi động service) để xác nhận các migration V8/V9/V10/V17/V4(crop) áp dụng đúng — đặc biệt kiểm tra bằng tay là bảng `certification_audits`, `certification_nonconformities`, `certification_corrective_actions`, `training_programs`, `employee_training_records` được **tạo ra thật** (không bị nuốt vào comment).
3. Bổ sung cột `shelf_life_days` còn thiếu trong `V4__add_storage_profile_to_crop.sql` (crop-catalog-service) cho khớp với `Crop.java`.
4. Viết/chạy lại CI Gate (`.github/workflows/ci-gate.yml` đã có sẵn matrix cho đúng 4/5 service này — trừ `delivery-service` không nằm trong matrix DB service hiện tại, cân nhắc bổ sung) để lần sau lỗi kiểu này bị chặn tự động thay vì phát hiện thủ công.
5. Sau khi backend build xanh thật sự: làm nốt phần thiếu của P4 (AI hook vào PesticideRecord), P6 (luồng checkout pre-order thực tế, hiện chỉ có cột DB), và **P8 (bổ sung field snapshot vào `MarketplaceTraceabilityResponse` + `PublicTracePage.tsx`)**.
6. Lên kế hoạch Frontend riêng cho toàn bộ tính năng mới P1/P2/P5/P6 — hiện là **con số 0** tuyệt đối, chưa tính vào P7.

---

## Phụ lục — Danh sách đầy đủ 61 file bị hỏng (literal `\r\n`)

```
crop-catalog-service/src/main/java/org/example/cropcatalog/dto/request/CropRequest.java
crop-catalog-service/src/main/java/org/example/cropcatalog/dto/response/CropResponse.java
crop-catalog-service/src/main/resources/db/migration/V4__add_storage_profile_to_crop.sql
delivery-service/src/main/java/org/example/delivery/dto/response/BatchSuggestionResponse.java
delivery-service/src/main/resources/db/migration/V10__seed_delivery_zones.sql
delivery-service/src/main/resources/db/migration/V9__add_batching_fields.sql
farm-service/src/main/java/org/example/farm/client/SeasonProductionDiaryClient.java
farm-service/src/main/java/org/example/farm/client/SeasonProductionDiaryClientFallback.java
farm-service/src/main/java/org/example/farm/controller/CertificationAuditController.java
farm-service/src/main/java/org/example/farm/dto/request/CompleteAuditRequest.java
farm-service/src/main/java/org/example/farm/dto/request/CreateAuditRequest.java
farm-service/src/main/java/org/example/farm/dto/request/CreateCorrectiveActionRequest.java
farm-service/src/main/java/org/example/farm/dto/request/CreateNonconformityRequest.java
farm-service/src/main/java/org/example/farm/dto/request/ExportDossierRequest.java
farm-service/src/main/java/org/example/farm/dto/request/IssueCertificateRequest.java
farm-service/src/main/java/org/example/farm/dto/request/ReviewCorrectiveActionRequest.java
farm-service/src/main/java/org/example/farm/dto/request/VerifyDocumentRequest.java
farm-service/src/main/java/org/example/farm/dto/response/CertificationAuditResponse.java
farm-service/src/main/java/org/example/farm/dto/response/CertificationInfoDto.java
farm-service/src/main/java/org/example/farm/dto/response/CorrectiveActionResponse.java
farm-service/src/main/java/org/example/farm/dto/response/NonconformityResponse.java
farm-service/src/main/java/org/example/farm/entity/CertificationAudit.java
farm-service/src/main/java/org/example/farm/entity/CertificationCorrectiveAction.java
farm-service/src/main/java/org/example/farm/entity/CertificationNonconformity.java
farm-service/src/main/java/org/example/farm/enums/AuditStatus.java
farm-service/src/main/java/org/example/farm/enums/AuditType.java
farm-service/src/main/java/org/example/farm/enums/CertificationRecordStatus.java
farm-service/src/main/java/org/example/farm/enums/CorrectiveActionReviewResult.java
farm-service/src/main/java/org/example/farm/enums/NonconformitySeverity.java
farm-service/src/main/java/org/example/farm/enums/NonconformityStatus.java
farm-service/src/main/java/org/example/farm/repository/CertificationAuditRepository.java
farm-service/src/main/java/org/example/farm/repository/CertificationCorrectiveActionRepository.java
farm-service/src/main/java/org/example/farm/repository/CertificationNonconformityRepository.java
farm-service/src/main/java/org/example/farm/service/CertificationAuditService.java
farm-service/src/main/resources/db/migration/V8__certification_audit_workflow.sql
marketplace-service/src/main/java/org/example/marketplace/controller/MarketplaceAdminController.java
marketplace-service/src/main/java/org/example/marketplace/dto/response/ComplianceCheckResponse.java
marketplace-service/src/main/java/org/example/marketplace/service/MarketplaceService.java
marketplace-service/src/main/java/org/example/marketplace/service/MarketplaceServiceImpl.java
marketplace-service/src/main/resources/db/migration/V8__add_compliance_gate_fields.sql
marketplace-service/src/main/resources/db/migration/V9__add_pre_order_fields.sql
season-service/src/main/java/org/example/season/client/SustainabilityServiceClient.java
season-service/src/main/java/org/example/season/client/SustainabilityServiceClientFallback.java
season-service/src/main/java/org/example/season/controller/DashboardController.java
season-service/src/main/java/org/example/season/controller/EmployeeTrainingController.java
season-service/src/main/java/org/example/season/controller/ProductionDiaryController.java
season-service/src/main/java/org/example/season/dto/request/EmployeeTrainingRecordRequest.java
season-service/src/main/java/org/example/season/dto/request/TrainingProgramRequest.java
season-service/src/main/java/org/example/season/dto/response/EmployeeTrainingRecordDto.java
season-service/src/main/java/org/example/season/dto/response/PHIAlertDto.java
season-service/src/main/java/org/example/season/dto/response/ProductionDiaryEventDto.java
season-service/src/main/java/org/example/season/dto/response/TrainingProgramDto.java
season-service/src/main/java/org/example/season/entity/EmployeeTrainingRecord.java
season-service/src/main/java/org/example/season/entity/TrainingProgram.java
season-service/src/main/java/org/example/season/repository/EmployeeTrainingRecordRepository.java
season-service/src/main/java/org/example/season/repository/TrainingProgramRepository.java
season-service/src/main/java/org/example/season/service/EmployeeTrainingService.java
season-service/src/main/java/org/example/season/service/PHIAlertService.java
season-service/src/main/java/org/example/season/service/ProductionDiaryAggregationService.java
season-service/src/main/resources/application.yml
season-service/src/main/resources/db/migration/V17__add_employee_training.sql
```

*(Ghi chú: `season-service/src/main/resources/application.yml` cũng dính lỗi — cần kiểm tra riêng vì đây là file cấu hình Spring Boot, hỏng file này có thể khiến service không đọc được config, kể cả khi mọi file `.java` khác đã được vá.)*
