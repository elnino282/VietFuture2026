# DISEASE_AI_BASELINE_V1

## 0) Scope, timing, and hard constraints
- Baseline scan time: `2026-05-02 06:45:51 +07:00`
- Scan scope completed: whole repo (`agricultural-crop-management-backend` + `agricultural-crop-management-frontend`)
- This document is the baseline for next prompts where **full-repo scan is no longer allowed**.
- No business code was changed in this scan.

## 1) Current architecture baseline

### Backend (Spring Boot, modular monolith)
- Base package: `org.example.QuanLyMuaVu`
- Pattern in active modules: `controller -> service -> repository/entity`, with partial `port` usage (query/command ports), DTO layers, and manual mapper classes.
- Relevant modules already present:
  - `module/season` (Season, FieldLog, Harvest, Labor/Payroll, Tasks)
  - `module/incident` (generic incident lifecycle)
  - `module/financial` (season expenses + expense search/list APIs)
  - `module/inventory` (SupplyItem/SupplyLot/Warehouse/InventoryBalance/StockMovement)
  - `module/ai` (Gemini chat + placeholder AI endpoints)
  - `module/sustainability` (farmer cost/revenue/profit/yield reports)
  - cross-cutting: `module/shared/config/SecurityConfig`, `Exception/GlobalExceptionHandler`, `module/farm/service/FarmAccessService`, `module/admin/service/AuditLogService`

### Frontend (React + Vite + TanStack Query)
- Routing: `src/app/routes.tsx` with farmer workspace under `/farmer/seasons/:seasonId/workspace/*`.
- Data access convention:
  - `entities/*/api/client.ts` (axios calls)
  - `entities/*/api/hooks.ts` (TanStack Query hooks)
  - `entities/*/model/schemas.ts` (Zod schema contracts)
- UI convention:
  - Domain pages/features in `src/features` and `src/pages`
  - Shared UI primitives through `src/shared/ui` (+ legacy usage of `src/components/ui`)
- Auth/session context:
  - `src/shared/api/http.ts` (JWT attach + refresh + account-lock handling)
  - `src/features/auth/context/AuthContext.tsx`
  - `src/shared/contexts/SeasonContext.tsx`

## 2) Feature-specific baseline (3 requested feature groups)

### 2.1 Disease tracking in season management
- Already available:
  - Field-level log timeline by season (`FieldLog`) with log type whitelist (`PEST`, `SPRAY`, etc.)
  - Generic incident tracking (`Incident`) with status/severity/deadline
  - Inventory entities for supplies/lots/movements
  - Expense entities/services by season/task
- Missing for disease history:
  - No structured disease record (diagnosis confidence, symptoms set, affected area/plant count, progression)
  - No structured treatment history (method/material/dose/cost/result per treatment step)
  - No disease evidence image upload flow in season/incident modules
  - No direct disease-to-stock-movement linkage model

### 2.2 AI suggestion from disease history + available supplies
- Already available:
  - Real Gemini integration at `POST /api/v1/farmer/ai/chat`
  - Inventory APIs provide stock context and item/lot metadata
  - Season/incident/field log/expense data accessible from backend services
- Gaps:
  - `AIController` suggestion/predict/optimize endpoints are placeholder payloads
  - FE `entities/ai` schemas/contracts expect different params than BE currently exposes
  - No policy layer enforcing “AI support only, no autonomous business mutation” at service boundary

### 2.3 AI cost optimization
- Already available:
  - Backend-first aggregation exists in farmer reports (`/api/v1/farmer/reports/{yield,cost,revenue,profit}`)
  - Expense, harvest, payroll, inventory/stock data exist
- Gaps:
  - No dedicated backend “cost optimization context summary” contract for AI
  - FE AI optimization widget still static (`AI_TIPS: []`)
  - FE expects expense analytics/budget-tracker/attachment endpoints that backend does not currently expose

## 3) Absolute must-read files for next prompts

### Backend must-read
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/entity/Season.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/entity/FieldLog.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/service/FieldLogService.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/controller/FieldLogController.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/incident/entity/Incident.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/incident/service/IncidentService.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/incident/controller/IncidentController.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/financial/entity/Expense.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/financial/service/SeasonExpenseService.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/inventory/entity/SupplyItem.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/inventory/entity/SupplyLot.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/inventory/entity/StockMovement.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/inventory/service/InventoryService.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/ai/controller/AIController.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/ai/controller/ChatController.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/ai/service/GeminiService.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/sustainability/service/FarmerReportService.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/shared/config/SecurityConfig.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/farm/service/FarmAccessService.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/Exception/GlobalExceptionHandler.java`
- `agricultural-crop-management-backend/src/main/resources/db/migration/B19__non_marketplace_core_baseline.sql`
- `agricultural-crop-management-backend/src/main/resources/application.properties`

### Frontend must-read
- `agricultural-crop-management-frontend/src/app/routes.tsx`
- `agricultural-crop-management-frontend/src/shared/api/http.ts`
- `agricultural-crop-management-frontend/src/shared/api/types.ts`
- `agricultural-crop-management-frontend/src/shared/contexts/SeasonContext.tsx`
- `agricultural-crop-management-frontend/src/entities/field-log/model/schemas.ts`
- `agricultural-crop-management-frontend/src/entities/field-log/api/client.ts`
- `agricultural-crop-management-frontend/src/entities/field-log/api/hooks.ts`
- `agricultural-crop-management-frontend/src/pages/farmer/FieldLogsPage.tsx`
- `agricultural-crop-management-frontend/src/entities/incident/model/schemas.ts`
- `agricultural-crop-management-frontend/src/entities/incident/api/client.ts`
- `agricultural-crop-management-frontend/src/pages/farmer/IncidentsPage.tsx`
- `agricultural-crop-management-frontend/src/features/farmer/season-workspace/SeasonWorkspaceLayout.tsx`
- `agricultural-crop-management-frontend/src/entities/ai/model/schemas.ts`
- `agricultural-crop-management-frontend/src/entities/ai/api/client.ts`
- `agricultural-crop-management-frontend/src/features/ai/hooks/useAiChatSession.ts`
- `agricultural-crop-management-frontend/src/pages/farmer/AiAssistantPage.tsx`
- `agricultural-crop-management-frontend/src/features/farmer/reports/hooks/useReports.ts`
- `agricultural-crop-management-frontend/src/features/farmer/reports/index.tsx`
- `agricultural-crop-management-frontend/src/features/farmer/expense-management/components/AIOptimizationTips.tsx`
- `agricultural-crop-management-frontend/src/entities/expense/api/client.ts`
- `agricultural-crop-management-frontend/src/entities/inventory/api/client.ts`
- `agricultural-crop-management-frontend/src/entities/supplies/api/client.ts`

## 4) Recommended extension boundaries

### Modules to extend (preferred)
- Backend:
  - `module/season` (for season-scoped disease lifecycle ownership checks)
  - new submodule or bounded context under `module/season` or `module/incident` for structured disease records/treatments
  - `module/inventory` (context queries for available supplies and stock)
  - `module/financial` (disease-treatment cost capture linkage)
  - `module/ai` (read-only recommendation/explanation endpoints)
  - `module/sustainability` (cost context summarization inputs)
- Frontend:
  - `entities/field-log`, `entities/incident`, `entities/ai`, `entities/inventory`, `entities/expense`
  - `pages/farmer/FieldLogsPage.tsx`, `features/farmer/season-workspace/*`, `pages/farmer/AiAssistantPage.tsx`, `features/farmer/expense-management/*`, `features/farmer/reports/*`

### Modules to avoid touching unless mandatory
- `module/marketplace/*`
- Firebase/chat infra outside farmer AI chat path
- Admin document/report modules not related to disease/cost flow
- Unrelated frontend buyer/admin/employee branches

## 5) Detected risks (build/contract/test)
- DB governance risk: `spring.jpa.hibernate.ddl-auto=update` while Flyway disabled by default (`spring.flyway.enabled=false` unless env override).
- API contract mismatch:
  - FE AI schemas use `seasonId/context`; BE AI placeholder endpoints use `crop/soil/season` or `area/crop`, `budget/crop`.
  - FE supplier CRUD endpoints (`/api/v1/supplies/suppliers/:id`, POST/PUT/DELETE) are not exposed by current `SuppliesController`.
  - FE expense attachment + analytics/budget tracker endpoints exist in client but no corresponding BE endpoints found.
- Farmer incidents page exists but `/farmer/incidents` route is not declared in `src/app/routes.tsx` (navigation risk).
- Encoding/mojibake appears in many comments/messages; avoid broad text refactor while implementing core features.
- Current AI tests cover chat + placeholder suggestion endpoint only; no disease-specific AI contract tests yet.

