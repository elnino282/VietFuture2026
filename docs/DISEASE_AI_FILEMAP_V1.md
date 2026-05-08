# DISEASE_AI_FILEMAP_V1

## Legend
- `EDIT`: được phép sửa ở các prompt tiếp theo.
- `READ`: chỉ đọc để lấy context/contract.
- `NO_TOUCH`: không đụng nếu không có lý do bắt buộc và explicit approval.
- Path shorthand used below:
  - `.../` trong bảng Backend = `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/`
  - `.../` trong bảng Frontend = `agricultural-crop-management-frontend/`

---

## A) Backend file map

### A1. Entity
| Path | Status | Notes |
|---|---|---|
| `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/entity/Season.java` | EDIT | season context cho disease/cost |
| `.../module/season/entity/FieldLog.java` | EDIT | có thể link disease record |
| `.../module/incident/entity/Incident.java` | EDIT | chỉ mở rộng nhẹ/soft-link |
| `.../module/financial/entity/Expense.java` | EDIT | link cost disease treatment |
| `.../module/season/entity/Harvest.java` | READ | cost/yield context |
| `.../module/season/entity/PayrollRecord.java` | READ | labor cost context |
| `.../module/inventory/entity/SupplyItem.java` | READ | AI supply recommendation context |
| `.../module/inventory/entity/SupplyLot.java` | READ | lot-level context |
| `.../module/inventory/entity/InventoryBalance.java` | READ | on-hand context |
| `.../module/inventory/entity/StockMovement.java` | READ | season/task usage context |
| `.../module/inventory/entity/Warehouse.java` | READ | warehouse scoping |
| `.../module/admin/entity/Document.java` | NO_TOUCH | không phải upload domain disease |

### A2. Repository
| Path | Status | Notes |
|---|---|---|
| `.../module/season/repository/FieldLogRepository.java` | EDIT | query disease-related logs |
| `.../module/season/repository/SeasonRepository.java` | READ | ownership/filters đã ổn |
| `.../module/incident/repository/IncidentRepository.java` | EDIT | nếu cần link disease record |
| `.../module/financial/repository/ExpenseRepository.java` | EDIT | disease-cost query |
| `.../module/inventory/repository/SupplyItemRepository.java` | READ | ai context |
| `.../module/inventory/repository/SupplyLotRepository.java` | READ | ai context |
| `.../module/inventory/repository/InventoryBalanceRepository.java` | READ | ai context |
| `.../module/inventory/repository/StockMovementRepository.java` | READ | ai context |
| `.../module/inventory/repository/WarehouseRepository.java` | READ | scoping |

### A3. Service
| Path | Status | Notes |
|---|---|---|
| `.../module/season/service/FieldLogService.java` | EDIT | disease entry integration |
| `.../module/incident/service/IncidentService.java` | EDIT | optional incident bridge |
| `.../module/financial/service/SeasonExpenseService.java` | EDIT | disease treatment cost link |
| `.../module/financial/service/ExpenseQueryService.java` | EDIT | cost aggregation feed |
| `.../module/inventory/service/InventoryService.java` | READ | stock validations/scope |
| `.../module/inventory/service/InventoryQueryService.java` | EDIT | supply context endpoints/support |
| `.../module/ai/service/GeminiService.java` | EDIT | prompt/context assembly usage |
| `.../module/sustainability/service/FarmerReportService.java` | EDIT | backend-first cost context |
| `.../module/farm/service/FarmAccessService.java` | READ | ownership enforcement |
| `.../module/admin/service/AuditLogService.java` | READ | audit integration reference |

### A4. Controller
| Path | Status | Notes |
|---|---|---|
| `.../module/season/controller/FieldLogController.java` | EDIT | disease-related endpoints/scope |
| `.../module/incident/controller/IncidentController.java` | EDIT | optional disease incident linking |
| `.../module/financial/controller/SeasonExpenseController.java` | EDIT | disease cost flows |
| `.../module/financial/controller/ExpenseController.java` | EDIT | list/filter consistency |
| `.../module/inventory/controller/InventoryController.java` | READ | existing inventory APIs |
| `.../module/inventory/controller/SuppliesController.java` | EDIT | if expose supplier/supply helper endpoints |
| `.../module/ai/controller/AIController.java` | EDIT | replace placeholders |
| `.../module/ai/controller/ChatController.java` | EDIT | context-enhanced chat input |
| `.../module/sustainability/controller/FarmerReportController.java` | EDIT | cost optimization source API |
| `.../module/admin/controller/*` | NO_TOUCH | ngoài scope feature lần này |
| `.../module/marketplace/controller/*` | NO_TOUCH | ngoài scope |

### A5. DTO / Mapper / Port
| Path | Status | Notes |
|---|---|---|
| `.../module/season/dto/request/CreateFieldLogRequest.java` | EDIT | disease fields only if strategy reuse |
| `.../module/season/dto/request/UpdateFieldLogRequest.java` | EDIT | idem |
| `.../module/season/dto/response/FieldLogResponse.java` | EDIT | extend response as needed |
| `.../module/incident/dto/request/CreateIncidentRequest.java` | EDIT | only if linking strategy |
| `.../module/incident/dto/request/IncidentStatusUpdateRequest.java` | READ | keep existing incident lifecycle |
| `.../module/incident/dto/response/IncidentResponse.java` | EDIT | optional disease reference |
| `.../module/financial/dto/request/CreateExpenseRequest.java` | EDIT | optional disease refs |
| `.../module/financial/dto/request/UpdateExpenseRequest.java` | EDIT | optional disease refs |
| `.../module/financial/dto/response/ExpenseResponse.java` | EDIT | optional disease refs |
| `.../module/season/mapper/SeasonMapper.java` | READ | mapping convention |
| `.../module/incident/mapper/IncidentMapper.java` | READ | mapping convention |
| `.../module/season/port/*.java` | READ | query/command contracts |
| `.../module/inventory/port/*.java` | READ | query contracts for AI context |

### A6. Migration / config
| Path | Status | Notes |
|---|---|---|
| `agricultural-crop-management-backend/src/main/resources/db/migration/B19__non_marketplace_core_baseline.sql` | READ | baseline schema reference |
| `agricultural-crop-management-backend/src/main/resources/db/migration/V6__season_expense_br_compliance.sql` | READ | expense historical schema changes |
| `agricultural-crop-management-backend/src/main/resources/db/migration/*.sql` | EDIT | thêm migration mới cho disease models |
| `agricultural-crop-management-backend/src/main/resources/application.properties` | READ | flyway/ddl risk awareness |
| `agricultural-crop-management-backend/src/main/resources/prompts/system_prompt.txt` | EDIT | AI guardrails + behavior tuning |

### A7. Exception / security / auth
| Path | Status | Notes |
|---|---|---|
| `.../Exception/GlobalExceptionHandler.java` | READ | error response contract |
| `.../Exception/ErrorCode.java` | EDIT | nếu cần bổ sung disease-specific error codes |
| `.../module/shared/config/SecurityConfig.java` | EDIT | chỉ khi thêm endpoint cần rule adjustment |
| `.../module/identity/*` | READ | auth/rbac existing flow |

### A8. Backend tests
| Path | Status | Notes |
|---|---|---|
| `agricultural-crop-management-backend/src/test/java/org/example/QuanLyMuaVu/module/ai/controller/AiModuleControllerIntegrationTest.java` | EDIT | update AI contracts |
| `agricultural-crop-management-backend/src/test/java/org/example/QuanLyMuaVu/module/inventory/controller/InventoryControllerSecurityTest.java` | READ | security baseline |
| `agricultural-crop-management-backend/src/test/java/org/example/QuanLyMuaVu/module/inventory/controller/InventorySuppliesModuleControllerIntegrationTest.java` | EDIT | supplier API contract changes if any |
| `agricultural-crop-management-backend/src/test/java/org/example/QuanLyMuaVu/module/incident/service/NotificationServiceTest.java` | READ | incident side-effect baseline |
| `agricultural-crop-management-backend/src/test/java/org/example/QuanLyMuaVu/module/season/controller/*` | EDIT | add disease-tracking tests (if integrated here) |
| New disease module tests (`agricultural-crop-management-backend/src/test/java/org/example/QuanLyMuaVu/module/*/disease/*Test.java`) | EDIT | expected in P0/P1 |

---

## B) Frontend file map

### B1. API layer
| Path | Status | Notes |
|---|---|---|
| `agricultural-crop-management-frontend/src/shared/api/http.ts` | READ | interceptor/auth refresh contract |
| `.../src/shared/api/types.ts` | READ | ApiResponse/PageResponse parser |
| `.../src/entities/field-log/api/client.ts` | EDIT | disease tracking contract alignment |
| `.../src/entities/field-log/api/hooks.ts` | EDIT | query/mutation invalidation |
| `.../src/entities/incident/api/client.ts` | EDIT | optional disease incident link |
| `.../src/entities/incident/api/hooks.ts` | EDIT | optional disease incident link |
| `.../src/entities/ai/api/client.ts` | EDIT | align BE AI contracts |
| `.../src/entities/ai/api/hooks.ts` | EDIT | new AI endpoints/hooks |
| `.../src/entities/expense/api/client.ts` | EDIT | cost optimization & disease cost links |
| `.../src/entities/inventory/api/client.ts` | READ | inventory context for AI |
| `.../src/entities/supplies/api/client.ts` | EDIT | supplier endpoint drift fixes if needed |
| `.../src/services/api.farmer.ts` | EDIT | report/cost API wiring |
| `.../src/services/api.ai.tsx` | READ | legacy TODO file (avoid reviving unless necessary) |

### B2. Types / schemas / keys
| Path | Status | Notes |
|---|---|---|
| `.../src/entities/field-log/model/schemas.ts` | EDIT | disease fields/schemas |
| `.../src/entities/field-log/model/types.ts` | EDIT | disease types |
| `.../src/entities/field-log/model/keys.ts` | EDIT | query key additions |
| `.../src/entities/incident/model/schemas.ts` | EDIT | optional disease links |
| `.../src/entities/incident/model/types.ts` | EDIT | optional disease links |
| `.../src/entities/ai/model/schemas.ts` | EDIT | AI disease/cost response contracts |
| `.../src/entities/ai/model/types.ts` | EDIT | AI disease/cost response contracts |
| `.../src/entities/expense/model/schemas.ts` | EDIT | disease-cost refs if added |

### B3. Hooks
| Path | Status | Notes |
|---|---|---|
| `.../src/features/ai/hooks/useAiChatSession.ts` | EDIT | attach richer context |
| `.../src/features/farmer/reports/hooks/useReports.ts` | EDIT | cost optimization data usage |
| `.../src/features/farmer/expense-management/hooks/useExpenseManagement.ts` | EDIT | disease-cost attachment flow |
| `.../src/shared/contexts/SeasonContext.tsx` | READ | season scoping behavior |
| `.../src/features/auth/context/AuthContext.tsx` | READ | auth baseline |

### B4. Pages
| Path | Status | Notes |
|---|---|---|
| `.../src/pages/farmer/FieldLogsPage.tsx` | EDIT | disease tracking UI integration |
| `.../src/pages/farmer/IncidentsPage.tsx` | EDIT | optional disease ticket bridge |
| `.../src/pages/farmer/AiAssistantPage.tsx` | EDIT | AI disease suggestion UX |
| `.../src/pages/farmer/InventoryPage.tsx` | READ | inventory interaction baseline |
| `.../src/pages/farmer/SuppliersSuppliesPage.tsx` | EDIT | if supplier contract fixed |
| `.../src/pages/ai/*.tsx` | NO_TOUCH | legacy placeholders, not active farmer route |

### B5. Components / feature screens
| Path | Status | Notes |
|---|---|---|
| `.../src/features/farmer/season-workspace/SeasonWorkspaceLayout.tsx` | EDIT | add disease workspace entry points |
| `.../src/features/farmer/season-workspace/SeasonWorkspaceOverview.tsx` | EDIT | summary widgets if needed |
| `.../src/features/farmer/season-workspace/SeasonReportsWorkspace.tsx` | EDIT | cost mode integration |
| `.../src/features/farmer/reports/index.tsx` | EDIT | cost optimization section wiring |
| `.../src/features/farmer/reports/components/*` | EDIT | visuals/kpi updates |
| `.../src/features/farmer/expense-management/components/AIOptimizationTips.tsx` | EDIT | replace static tips |
| `.../src/shared/ui/*` | READ | keep UI conventions |
| `.../src/components/ui/*` | READ | legacy UI primitives; avoid broad refactor |

### B6. Routes
| Path | Status | Notes |
|---|---|---|
| `.../src/app/routes.tsx` | EDIT | expose `/farmer/incidents` if needed + workspace disease route |
| `.../src/App.tsx` | READ | provider stack baseline |

### B7. Frontend tests
| Path | Status | Notes |
|---|---|---|
| `.../src/tests/AiChat.test.tsx` | EDIT | currently placeholder TODO |
| `.../src/tests/ExpenseManagement.test.tsx` | EDIT | extend cost optimization assertions |
| `.../src/tests/useExpenseManagement.test.tsx` | EDIT | disease-cost flow tests |
| `.../src/features/farmer/reports/components/KPICards.test.tsx` | EDIT | KPI regression checks |
| `.../src/features/farmer/season-workspace/*.test.tsx` | EDIT | workspace integration checks |
| unrelated admin/buyer tests | READ | no direct scope |

---

## C) Explicit NO_TOUCH zones (unless mandatory)
- Backend:
  - `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/marketplace/**`
  - `.../module/admin/**` (trừ audit/error code very targeted)
  - `.../module/firebase/**`
- Frontend:
  - `agricultural-crop-management-frontend/src/features/marketplace/**`
  - `.../src/features/admin/**` (trừ test contract accidental break detection)
  - `.../src/features/buyer/**`
  - `.../src/features/employee/**`
