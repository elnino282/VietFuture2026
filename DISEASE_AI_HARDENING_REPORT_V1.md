# DISEASE_AI_HARDENING_REPORT_V1

## 1) Cac van de phat hien

1. Frontend Disease form/treatment form truoc day cho phep nhap so khong hop le (chuoi khong phai so, so am, so thap phan cho truong integer) va silently bo qua field khi submit.
2. Frontend thong diep loi API chu yeu lay `error.message`, chua map ro loi quyen (401/403) va loi validation theo convention backend.
3. Backend AI request DTO (disease suggestion + season cost optimization suggestion) chua co gioi han do dai input o server side, du frontend da co zod.
4. WebMvc security tests moi them (EMPLOYEE -> FORBIDDEN) ban dau khong co method-security trong test slice, dan den false-positive 200. Da harden test config de assertion RBAC co gia tri.

## 2) Cac file da sua

### Backend
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/ai/dto/request/DiseaseSuggestionRequest.java`
  - Them `@Size(max = 2000)` cho `question`, `@Size(max = 4000)` cho `additionalNote`.
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/ai/dto/request/SeasonCostOptimizationSuggestionRequest.java`
  - Them `@Size(max = 2000)` cho `question`, `@Size(max = 4000)` cho `additionalNote`.
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/controller/DiseaseRecordController.java`
  - Them `@Valid` cho body endpoint `POST /api/v1/disease-records/{id}/ai-suggestion`.
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/controller/SeasonCostOptimizationController.java`
  - Them `@Valid` cho body endpoint `POST /api/v1/seasons/{seasonId}/cost-optimization/ai-suggestion`.
- `agricultural-crop-management-backend/src/test/java/org/example/QuanLyMuaVu/module/season/controller/DiseaseRecordControllerAiSuggestionTest.java`
  - Them test role guard EMPLOYEE bi chan.
  - Them test validation payload qua dai (400).
  - Them `@EnableMethodSecurity` test config cho WebMvc slice.
- `agricultural-crop-management-backend/src/test/java/org/example/QuanLyMuaVu/module/season/controller/SeasonCostOptimizationControllerTest.java`
  - Them test role guard EMPLOYEE bi chan cho summary + ai suggestion.
  - Them test validation payload qua dai (400).
  - Them `@EnableMethodSecurity` test config cho WebMvc slice.

### Frontend
- `agricultural-crop-management-frontend/src/pages/farmer/DiseaseTrackingPage.tsx`
  - Hardening parse/validation cho cac truong so:
    - `affectedPlantCount` phai la integer >= 0.
    - `affectedAreaPercent`, `quantityUsed`, `costAmount` phai >= 0.
  - Chan submit neu form invalid (`disabled` tren submit buttons + guard trong handler).
  - Them inline validation message de user thay loi nhap lieu ro rang.
  - Kiem tra datetime hop le truoc submit.
  - Hardening thong diep loi API: map 401/403/400 + error code backend sang thong diep than thien.
  - Khi tai detail/treatment fail, hien thong diep loi da map thay vi message chung.
  - Them guard do dai `additionalNote` AI (toi da 4000 ky tu).
- `agricultural-crop-management-frontend/src/features/farmer/reports/hooks/useReports.ts`
  - Them `toReadableApiError` de map loi quyen/validation cho Cost Optimization summary + AI error.
- `agricultural-crop-management-frontend/src/services/api.farmer.ts`
  - Hardening schema request Cost Optimization AI: `question.max(2000)`, `additionalNote.max(4000)`.

## 3) Cac rule bao mat da dam bao

1. RBAC endpoint layer:
   - Disease endpoints va Cost Optimization endpoints van duoc bao ve boi `@PreAuthorize("hasRole('FARMER')")`.
   - Da co test explicit cho case EMPLOYEE bi FORBIDDEN (WebMvc test + method security enabled trong test context).
2. Ownership scope:
   - Disease record/treatment va AI disease suggestion tiep tuc check ownership qua `farmAccessService.assertCurrentUserCanAccessSeason(...)` trong service.
   - Cost optimization summary/AI suggestion tiep tuc check ownership season qua `farmAccessService.assertCurrentUserCanAccessSeason(...)`.
3. Validation:
   - Backend: severity/status/effectiveness/date/cost/quantity/supply checks giu nguyen theo logic hien co.
   - Backend AI request duoc bo sung gioi han do dai payload.
   - Frontend: form khong cho submit du lieu so invalid/so am.
4. Audit/consistency:
   - Audit cho create/update/delete disease record/treatment va AI requests van duoc giu nguyen theo `AuditLogService`.
   - Khong them auto stock deduction/auto expense mutation; hanh vi van la reference/link an toan.
5. Error handling:
   - Backend tra loi theo convention `ApiResponse + ErrorCode`, khong tra stack trace cho client.
   - Frontend map thong diep loi quyen/validation than thien hon.

## 4) Lenh verify va ket qua

### Backend (thu muc `agricultural-crop-management-backend`)
1. `./mvnw.cmd clean -DskipTests compile`
   - Ket qua: `SUCCESS`.
2. `./mvnw.cmd "-Dtest=DiseaseRecordControllerAiSuggestionTest,SeasonCostOptimizationControllerTest" test`
   - Ket qua: `SUCCESS`.
   - Tests run: 8, Failures: 0, Errors: 0.

### Frontend (thu muc `agricultural-crop-management-frontend`)
1. `npm run typecheck`
   - Ket qua: `FAILED` (pre-existing, ngoai scope hardening nay).
   - Loi ton tai: `src/shared/api/marketplace/mock-adapter.ts:910` (`MarketplaceTraceability.lot.initialQuantity` mismatch).
2. `npm run build`
   - Ket qua: `SUCCESS`.
   - Con warning pre-existing ve circular re-export `useAuth` va chunk size.

## 5) Loi con ton tai

1. Frontend typecheck toan bo repo van fail do loi pre-existing o marketplace mock adapter (`initialQuantity` missing).
2. Frontend build co warning pre-existing:
   - circular dependency/re-export lien quan `useAuth`.
   - chunk lon > 500 kB.
3. Backend co warning deprecation trong test suite (`@MockBean` deprecations), khong chan build.

## 6) Prompt tiep theo nen gui

```txt
Tiep tuc phase hardening tiep theo (khong mo rong feature):
1) Sua loi typecheck pre-existing tai src/shared/api/marketplace/mock-adapter.ts: bo sung du field lot.initialQuantity de pass tsc.
2) Bo sung service-level tests cho ownership denial (FarmAccessPort throw FORBIDDEN) cho:
   - DiseaseRecordService (record/treatment/ai related paths)
   - SeasonCostOptimizationService (summary + ai suggestion)
3) Giu nguyen API contract hien tai, chi them test/validation hoac fix logic neu co rui ro security/consistency ro rang.
```
