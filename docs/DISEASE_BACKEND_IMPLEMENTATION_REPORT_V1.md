# DISEASE_BACKEND_IMPLEMENTATION_REPORT_V1

## 1) File da them/sua

### Backend code
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/Enums/DiseaseSeverity.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/Enums/DiseaseStatus.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/Enums/TreatmentEffectiveness.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/Exception/ErrorCode.java` (updated)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/entity/DiseaseRecord.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/entity/DiseaseTreatment.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/repository/DiseaseRecordRepository.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/repository/DiseaseTreatmentRepository.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/dto/request/CreateDiseaseRecordRequest.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/dto/request/UpdateDiseaseRecordRequest.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/dto/request/CreateDiseaseTreatmentRequest.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/dto/request/UpdateDiseaseTreatmentRequest.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/dto/response/DiseaseRecordResponse.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/dto/response/DiseaseRecordDetailResponse.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/dto/response/DiseaseTreatmentResponse.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/mapper/DiseaseRecordMapper.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/service/DiseaseRecordService.java` (new)
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/controller/DiseaseRecordController.java` (new)

### Migration
- `agricultural-crop-management-backend/src/main/resources/db/migration/V21__disease_tracking_module.sql` (new)

## 2) API da implement

Base path: `/api/v1`  
Security: `@PreAuthorize("hasRole('FARMER')")`

- `GET /seasons/{seasonId}/disease-records`
- `POST /seasons/{seasonId}/disease-records`
- `GET /disease-records/{id}`
- `PUT /disease-records/{id}`
- `DELETE /disease-records/{id}`
- `GET /disease-records/{id}/treatments`
- `POST /disease-records/{id}/treatments`
- `PUT /disease-treatments/{id}`
- `DELETE /disease-treatments/{id}`

Response convention da bam sat he thong hien co:
- `ApiResponse<T>`
- `PageResponse<T>` cho list endpoint

## 3) Migration da them

File: `V21__disease_tracking_module.sql`

- Tao bang `disease_records`
- Tao bang `disease_treatments`
- FK theo convention hien co:
  - `disease_records.season_id -> seasons.season_id`
  - `disease_records.plot_id -> plots.plot_id`
  - `disease_records.crop_id -> crops.crop_id`
  - `disease_records.variety_id -> varieties.id`
  - `disease_records.reported_by_user_id -> users.user_id`
  - `disease_records.incident_id -> incidents.id`
  - `disease_treatments.disease_record_id -> disease_records.disease_record_id`
  - `disease_treatments.supply_item_id -> supply_items.id`
  - `disease_treatments.supply_lot_id -> supply_lots.id`
  - `disease_treatments.expense_id -> expenses.expense_id`
  - `disease_treatments.created_by_user_id -> users.user_id`
- Indexes:
  - `idx_disease_records_season_id`
  - `idx_disease_records_status`
  - `idx_disease_records_severity`
  - `idx_disease_records_detected_at`
  - `idx_disease_treatments_disease_record_id`

## 4) Validation/RBAC da xu ly

### Validation
- DTO level (`jakarta.validation`):
  - Bat buoc: `diseaseName`, `severity`, `detectedAt`, `treatedAt`, `method`
  - Gioi han do dai cac text field theo contract
  - So luong/chi phi khong am: `affectedPlantCount`, `affectedAreaValue`, `quantityUsed`, `costAmount`
- Service level:
  - Parse enum hop le: severity/status/effectiveness
  - `detectedAt` phai nam trong khoang thoi gian mua vu
  - `treatedAt` khong duoc nho hon `detectedAt`
  - `incidentId`/`expenseId` phai cung season voi disease record
  - `supplyLotId` neu co phai truy cap duoc theo farm ownership va khop `supplyItemId` neu cung gui len

### RBAC + ownership
- Controller khoa role FARMER (`@PreAuthorize("hasRole('FARMER')")`)
- Moi thao tac doc/ghi deu goi `FarmAccessPort.assertCurrentUserCanAccessSeason(...)`
- Khong cho CRUD khi season da dong (`COMPLETED`, `CANCELLED`, `ARCHIVED`)

### Delete pattern
- Dang ap dung hard delete theo pattern hien tai module lien quan:
  - Xoa disease record se xoa treatment lien quan truoc
  - Xoa treatment la hard delete

## 5) Lenh verify da chay va ket qua

### Compile
```bash
./mvnw -DskipTests compile
```
Ket qua: `BUILD SUCCESS`.

### Targeted tests (season controllers)
```bash
./mvnw "-Dtest=FarmerLaborManagementControllerTest,EmployeePortalControllerTest" test
```
Ket qua: `BUILD SUCCESS` (7 tests run, 0 failures, 0 errors).

### Targeted Flyway reproducibility test
```bash
./mvnw "-Dtest=FlywayNonMarketplaceReproducibilityTest" test
```
Ket qua: `BUILD FAILURE` do moi truong local khong co MySQL test tai `127.0.0.1:33306` (connection refused), khong phai loi compile/module disease moi.

## 6) Loi con ton tai

- Chua co test unit/integration rieng cho `DiseaseRecordService` va `DiseaseRecordController`.
- Flyway reproducibility test dang phu thuoc DB test local (`flyway_repro`) nen fail neu chua bat MySQL tai port 33306.
- Khong co soft delete cho disease module (co chu y theo yeu cau va convention hien tai).

## 7) Prompt tiep theo nen gui

De xong P0 backend o muc production-ready, prompt tiep theo nen yeu cau:

1. Them test cho Disease Tracking backend:
   - Service tests cho ownership/validation/date rules/supply-expense-season checks.
   - Controller security tests cho role FARMER va forbidden cases.
2. Chay lai targeted test disease + compile.
3. Cap nhat docs contract neu can (chi khi test phat hien mismatch nho).
