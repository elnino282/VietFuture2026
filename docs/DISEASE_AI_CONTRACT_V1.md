# DISEASE_AI_CONTRACT_V1

## 0) Decision Summary (Locked for Implementation)
- Chosen approach: **new dedicated domain tables** `disease_records` + `disease_treatments`.
- Keep `Incident` as optional bridge only, not primary storage for disease history.
- Keep API conventions aligned with current backend:
  - `ApiResponse<T>` wrapper for all endpoints.
  - `PageResponse<T>` for list endpoints.
  - `@PreAuthorize("hasRole('FARMER')")` + service-level ownership checks via season access.
- Keep delete behavior aligned with current FieldLog/Incident/Expense: **hard delete in V1** (no soft delete columns).
- AI is advisory only: no business data mutation by AI endpoints.

---

## 1) Entity / Table Design

## 1.1 `disease_records`
Purpose: one disease event observed in one season (with optional seasonal snapshots and optional incident linkage).

Suggested columns:
- `id` INT PK AUTO_INCREMENT
- `season_id` INT NOT NULL FK -> `seasons.season_id`
- `plot_id` INT NULL FK -> `plots.plot_id` (snapshot for faster query/filter)
- `crop_id` INT NULL FK -> `crops.crop_id` (snapshot)
- `variety_id` INT NULL FK -> `varieties.id` (snapshot)
- `reported_by_user_id` BIGINT NOT NULL FK -> `users.user_id`
- `incident_id` INT NULL FK -> `incidents.id` (optional bridge)
- `disease_name` VARCHAR(150) NOT NULL
- `symptom_summary` TEXT NULL
- `severity` VARCHAR(20) NOT NULL
- `status` VARCHAR(30) NOT NULL
- `detected_at` DATETIME NOT NULL
- `affected_plant_count` INT NULL
- `affected_area_value` DECIMAL(14,3) NULL
- `affected_area_unit` VARCHAR(20) NULL
- `evidence_url` VARCHAR(1000) NULL
- `notes` TEXT NULL
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP NULL

Indexes:
- `idx_disease_records_season_created` (`season_id`, `created_at` DESC)
- `idx_disease_records_status` (`status`)
- `idx_disease_records_severity` (`severity`)
- `idx_disease_records_reported_by` (`reported_by_user_id`)

Notes:
- Snapshot fields (`plot_id`, `crop_id`, `variety_id`) are copied from season at create time to reduce join dependency and keep historical context stable.
- `incident_id` is optional and must belong to the same `season_id` if provided.

## 1.2 `disease_treatments`
Purpose: treatment history entries for each disease record.

Suggested columns:
- `id` INT PK AUTO_INCREMENT
- `disease_record_id` INT NOT NULL FK -> `disease_records.id`
- `treated_at` DATETIME NOT NULL
- `method` VARCHAR(100) NOT NULL
- `supply_item_id` INT NULL FK -> `supply_items.id`
- `supply_lot_id` INT NULL FK -> `supply_lots.id`
- `material_name` VARCHAR(150) NULL (snapshot/manual when no lot/item)
- `quantity_used` DECIMAL(14,3) NULL
- `unit` VARCHAR(20) NULL
- `cost_amount` DECIMAL(19,2) NULL
- `expense_id` INT NULL FK -> `expenses.expense_id`
- `effectiveness` VARCHAR(20) NULL
- `result_summary` TEXT NULL
- `next_review_at` DATE NULL
- `notes` TEXT NULL
- `created_by_user_id` BIGINT NOT NULL FK -> `users.user_id`
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP NULL

Indexes:
- `idx_disease_treatments_record_created` (`disease_record_id`, `created_at` DESC)
- `idx_disease_treatments_supply_lot` (`supply_lot_id`)
- `idx_disease_treatments_expense` (`expense_id`)

## 1.3 Enum/Code Strategy
To minimize structural risk, keep DB as `VARCHAR` + service-level whitelist validation (same style as current modules using `String` + domain checks).

Recommended controlled values:
- Disease severity: `LOW | MEDIUM | HIGH | CRITICAL`
- Disease status: `OPEN | UNDER_TREATMENT | MONITORING | RESOLVED | CLOSED`
- Treatment effectiveness: `UNKNOWN | POOR | FAIR | GOOD | EXCELLENT`

---

## 2) API Design

All endpoints:
- Base: `/api/v1`
- Role: `FARMER`
- Response envelope: `ApiResponse`

## 2.1 Disease Record Endpoints

1. `GET /api/v1/seasons/{seasonId}/disease-records`
- Purpose: list disease records of season.
- Query params:
  - `status` (optional)
  - `severity` (optional)
  - `q` (optional, search in disease name/symptom)
  - `fromDetectedAt` (optional, `yyyy-MM-dd`)
  - `toDetectedAt` (optional, `yyyy-MM-dd`)
  - `page` default `0`
  - `size` default `20`
- Response: `ApiResponse<PageResponse<DiseaseRecordResponse>>`

2. `POST /api/v1/seasons/{seasonId}/disease-records`
- Purpose: create a record under season.
- Body: `CreateDiseaseRecordRequest`
- Response: `ApiResponse<DiseaseRecordResponse>`

3. `GET /api/v1/disease-records/{id}`
- Purpose: detail (record + treatment summary).
- Response: `ApiResponse<DiseaseRecordDetailResponse>`

4. `PUT /api/v1/disease-records/{id}`
- Purpose: update editable fields.
- Body: `UpdateDiseaseRecordRequest`
- Response: `ApiResponse<DiseaseRecordResponse>`

5. `DELETE /api/v1/disease-records/{id}`
- V1 behavior: hard delete.
- Response: `ApiResponse<Void>`

## 2.2 Disease Treatment Endpoints

6. `GET /api/v1/disease-records/{id}/treatments`
- Query params: `page`, `size`
- Response: `ApiResponse<PageResponse<DiseaseTreatmentResponse>>`

7. `POST /api/v1/disease-records/{id}/treatments`
- Body: `CreateDiseaseTreatmentRequest`
- Response: `ApiResponse<DiseaseTreatmentResponse>`

8. `PUT /api/v1/disease-treatments/{id}`
- Body: `UpdateDiseaseTreatmentRequest`
- Response: `ApiResponse<DiseaseTreatmentResponse>`

9. `DELETE /api/v1/disease-treatments/{id}`
- V1 behavior: hard delete.
- Response: `ApiResponse<Void>`

---

## 3) DTO Contract

## 3.1 Request DTOs

### `CreateDiseaseRecordRequest`
- `diseaseName` string required, max 150
- `symptomSummary` string optional, max 4000
- `severity` string required (whitelist)
- `status` string optional (default `OPEN`)
- `detectedAt` ISO datetime required
- `affectedPlantCount` integer optional, min 0
- `affectedAreaValue` decimal optional, min 0
- `affectedAreaUnit` string optional, max 20
- `evidenceUrl` string optional, max 1000
- `notes` string optional, max 4000
- `incidentId` integer optional

### `UpdateDiseaseRecordRequest`
- same as create, all optional except business-required fields for consistency:
  - if present, validate with same rules.

### `CreateDiseaseTreatmentRequest`
- `treatedAt` ISO datetime required
- `method` string required, max 100
- `supplyItemId` integer optional
- `supplyLotId` integer optional
- `materialName` string optional, max 150
- `quantityUsed` decimal optional, min 0
- `unit` string optional, max 20
- `costAmount` decimal optional, min 0
- `expenseId` integer optional
- `effectiveness` string optional (whitelist)
- `resultSummary` string optional, max 4000
- `nextReviewAt` date optional
- `notes` string optional, max 4000

### `UpdateDiseaseTreatmentRequest`
- same as create, all optional with same validation.

## 3.2 Response DTOs

### `DiseaseRecordResponse`
- `id`
- `seasonId`
- `seasonName`
- `plotId`, `plotName`
- `cropId`, `cropName`
- `varietyId`, `varietyName`
- `reportedByUserId`, `reportedByUsername`
- `incidentId`
- `diseaseName`
- `symptomSummary`
- `severity`
- `status`
- `detectedAt`
- `affectedPlantCount`
- `affectedAreaValue`
- `affectedAreaUnit`
- `evidenceUrl`
- `notes`
- `createdAt`, `updatedAt`
- `treatmentCount` (optional computed)
- `latestTreatmentAt` (optional computed)

### `DiseaseTreatmentResponse`
- `id`
- `diseaseRecordId`
- `treatedAt`
- `method`
- `supplyItemId`, `supplyItemName`
- `supplyLotId`, `batchCode`
- `materialName`
- `quantityUsed`
- `unit`
- `costAmount`
- `expenseId`
- `effectiveness`
- `resultSummary`
- `nextReviewAt`
- `notes`
- `createdByUserId`, `createdByUsername`
- `createdAt`, `updatedAt`

### `DiseaseRecordDetailResponse`
- all fields from `DiseaseRecordResponse`
- `treatments`: `DiseaseTreatmentResponse[]` (option A)
- OR paged treatments separately by endpoint (option B, preferred for scalability)
- `totalTreatmentCost` (optional aggregated value)

---

## 4) Validation Rules

## 4.1 Ownership and Season Scope
- `seasonId` in path is mandatory.
- Current farmer must have access to season via farm ownership (`FarmAccessService.assertCurrentUserCanAccessSeason`).
- Record/treatment by ID must resolve back to a season that current user can access.

## 4.2 Date/Time
- `detectedAt` required on create.
- `detectedAt` must be within season logical range:
  - not before `season.startDate`
  - not after `season.endDate` (or `plannedHarvestDate` if endDate is null)
- `treatedAt` should not be before `detectedAt` of parent record (recommended hard validation).

## 4.3 Status/Severity/Effectiveness
- Reject values outside whitelist.
- Normalize to uppercase before store.

## 4.4 Numeric
- `affectedPlantCount >= 0`
- `affectedAreaValue >= 0`
- `quantityUsed >= 0`
- `costAmount >= 0`

## 4.5 Cross-reference Integrity
- `incidentId` if provided must belong to same season and be accessible.
- `expenseId` if provided must be accessible and belong to same season as disease record.
- `supplyLotId` if provided must be accessible in farmer scope (farm/warehouse ownership constraints similar to inventory flows).
- if both `supplyLotId` and `supplyItemId` provided, lot must match item.

## 4.6 Write Lock
To stay consistent with current Season-dependent modules:
- deny create/update/delete when season status is `COMPLETED | CANCELLED | ARCHIVED`.

## 4.7 Delete Behavior
- V1 uses hard delete.
- Delete disease record should delete child treatments in one transaction (FK cascade or service-level explicit delete).

---

## 5) RBAC and Ownership Rules

Controller level:
- `@PreAuthorize("hasRole('FARMER')")`

Service level:
- Access season through repository/port and enforce `FarmAccessService.assertCurrentUserCanAccessSeason`.
- For ID-based read/write, resolve parent season first, then enforce access.
- `reportedByUserId` and `createdByUserId` are always taken from authenticated user, never trusted from request body.

---

## 6) Audit / Logging Rules

Use existing `AuditLogService.logModuleOperation` with module prefixing.

Recommended audit events:
- `DISEASE_RECORD_CREATED`
- `DISEASE_RECORD_UPDATED`
- `DISEASE_RECORD_DELETED`
- `DISEASE_TREATMENT_CREATED`
- `DISEASE_TREATMENT_UPDATED`
- `DISEASE_TREATMENT_DELETED`

Recommended entity types:
- `DISEASE_RECORD`
- `DISEASE_TREATMENT`

Snapshot payload guidance:
- include IDs, key status/severity/timestamps, supply/expense references
- avoid sensitive personal fields

---

## 7) Frontend UI/API Contract

## 7.1 FE API Envelope
Must stay compatible with existing parser layer:
- `ApiResponse<T> { status, code, message, result }`
- List result must be `PageResponse<T> { items, page, size, totalElements, totalPages }`

## 7.2 FE Entity Module (recommended new)
Create dedicated FE domain module (later prompt):
- `src/entities/disease/api/client.ts`
- `src/entities/disease/api/hooks.ts`
- `src/entities/disease/model/schemas.ts`
- `src/entities/disease/model/types.ts`
- `src/entities/disease/model/keys.ts`

## 7.3 FE Query Keys
- `diseaseKeys.listBySeason(seasonId, params)`
- `diseaseKeys.detail(id)`
- `diseaseKeys.treatments(recordId, params)`

Invalidation rules:
- create/update/delete record => invalidate season disease list + detail
- create/update/delete treatment => invalidate treatment list + record detail

## 7.4 UI Integration Points
- Season workspace entry: disease tab/page scoped by selected season.
- Primary list screen:
  - filters: severity/status/date range/search
  - create/edit/delete record
- Record detail:
  - treatment history list
  - add/edit/delete treatment
- Optional relation chips:
  - linked incident
  - linked expense
  - linked supply lot/item

## 7.5 Upload Strategy (V1)
- Do not block create/update on image upload availability.
- Use `evidenceUrl` text field first.
- Binary upload endpoint integration is deferred until upload pipeline is confirmed stable.

---

## 8) Open Checks Before Coding (Must Verify in Implementation Prompt)

1. **Flyway strategy conflict**
- Current runtime has `spring.jpa.hibernate.ddl-auto=update` and Flyway disabled by default.
- Need explicit team decision: migration-first (`V21__...`) vs relying on JPA auto-update in local.

2. **API mismatch already exists in AI/Expense fronts**
- Current FE AI suggestion/cost contracts do not match current BE placeholder payloads.
- Must avoid coupling Disease contract with old AI placeholder response shape.

3. **Supply lot accessibility check reuse**
- Decide whether to expose reusable validation through inventory port/service or duplicate limited checks in disease service.

4. **Expense linkage semantics**
- Decide if treatment `expenseId` is required when `costAmount` is present, or optional snapshot-only cost is allowed.

5. **Datetime format and timezone**
- FE should submit ISO datetime for `detectedAt` / `treatedAt`; confirm backend parser and timezone expectation.

6. **Incident bridge behavior**
- Decide whether creating disease record auto-creates incident (not recommended in V1), or incident link is manual.

---

## 9) Implementation Bias (Low-risk Path)
- Build disease as isolated module first (`entity/repository/service/controller/dto`).
- Reuse existing season ownership and season-lock validations.
- Keep Incident/Expense/Inventory integrations optional-by-reference, not hard-coupled workflows.
- Add AI usage on top of disease/expense/inventory read models after disease tracking CRUD is stable.
