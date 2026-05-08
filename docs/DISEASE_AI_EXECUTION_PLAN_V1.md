# DISEASE_AI_EXECUTION_PLAN_V1

## 0) Execution rules for all next prompts
- Không scan toàn repo nữa.
- Chỉ đọc/sửa file nằm trong `docs/DISEASE_AI_FILEMAP_V1.md`, và chỉ subset của prompt hiện tại.
- Không đổi kiến trúc thư mục, không refactor lớn, không đổi naming convention trừ khi bắt buộc kỹ thuật.
- AI endpoints chỉ hỗ trợ quyết định; không tự mutate dữ liệu nghiệp vụ.

---

## Prompt 1 (P0) — Disease Tracking Backend

### Mục tiêu
- Thêm mô hình dữ liệu disease tracking có cấu trúc + treatment history.
- Expose farmer CRUD APIs theo season ownership/RBAC hiện tại.

### Được phép đọc/sửa
- Backend only:
  - `module/season/*` (field-log/season ownership flow liên quan)
  - `module/incident/*` (chỉ phần liên kết mềm nếu cần)
  - `module/financial/*` (nếu cần link disease cost)
  - `module/inventory/*` (dữ liệu vật tư điều trị)
  - `module/shared/config/SecurityConfig.java`
  - `Exception/*`
  - `db/migration/*` (migration mới)
  - test files trong `src/test/java/org/example/QuanLyMuaVu/module/*` liên quan

### Không được làm
- Không sửa marketplace/docs admin modules.
- Không scan frontend ở prompt này (trừ khi cần xác nhận contract file cụ thể đã whitelist trong filemap).

### Output mong đợi
- Entity/DTO/Repository/Service/Controller cho disease record + treatment history.
- Migration SQL mới.
- API contract rõ cho frontend.

### Verify commands
```powershell
cd agricultural-crop-management-backend
./mvnw -q -DskipTests compile
./mvnw -q test
```

---

## Prompt 2 (P0) — Disease Tracking Frontend

### Mục tiêu
- Tích hợp UI disease tracking vào Season Workspace.
- Form nhập bệnh + treatment timeline + liên kết season/inventory/expense context.

### Được phép đọc/sửa
- Frontend only:
  - `src/entities/field-log/*`
  - `src/entities/incident/*`
  - disease entities mới (nếu tạo)
  - `src/features/farmer/season-workspace/*`
  - `src/pages/farmer/FieldLogsPage.tsx`
  - `src/pages/farmer/IncidentsPage.tsx` (nếu route đã bật)
  - `src/app/routes.tsx` (nếu cần expose route bệnh)
  - shared UI/components tối thiểu cần dùng

### Không được làm
- Không scan backend ngoài các API contract file đã xác định.
- Không refactor toàn bộ design system.

### Output mong đợi
- Màn hình/section disease tracking hoạt động với API mới.
- State/query invalidation theo TanStack Query convention hiện có.

### Verify commands
```powershell
cd agricultural-crop-management-frontend
npm run typecheck
npm run lint
npm run test -- --run
npm run build
```

---

## Prompt 3 (P1) — AI Disease Suggestion Backend

### Mục tiêu
- Tạo endpoint AI disease suggestion dùng context disease history + inventory available.
- Enforce read-only AI behavior.

### Được phép đọc/sửa
- `module/ai/*`
- disease backend files từ Prompt 1
- `module/inventory/*` (query context only)
- `module/season/*`, `module/incident/*`, `module/financial/*` (read/context aggregation)
- `resources/prompts/system_prompt.txt` hoặc prompt files mới
- tests AI controller/service

### Không được làm
- Không cho AI trigger write command services.

### Output mong đợi
- Endpoint ví dụ: `/api/v1/farmer/ai/disease-suggestions`
- Response có disclaimer + confidence + supply-backed options.

### Verify commands
```powershell
cd agricultural-crop-management-backend
./mvnw -q -DskipTests compile
./mvnw -q -Dtest=AiModuleControllerIntegrationTest test
```

---

## Prompt 4 (P1) — AI Disease Suggestion Frontend

### Mục tiêu
- Kết nối UI assistant với endpoint disease suggestion mới.
- Hiển thị gợi ý có ưu tiên vật tư đang có trong kho.

### Được phép đọc/sửa
- `src/entities/ai/*`
- `src/features/ai/hooks/useAiChatSession.ts`
- `src/pages/farmer/AiAssistantPage.tsx`
- disease UI files từ Prompt 2 nếu cần gọi suggestion từ context hiện tại

### Không được làm
- Không scan/đụng các trang AI placeholder cũ ngoài phạm vi cần thiết.

### Output mong đợi
- Contract frontend-backend aligned (params/result schemas).
- UI hiển thị rõ “AI hỗ trợ, không thay thế chuyên gia”.

### Verify commands
```powershell
cd agricultural-crop-management-frontend
npm run typecheck
npm run lint
npm run test -- --run src/tests/AiChat.test.tsx
npm run build
```

---

## Prompt 5 (P1) — Cost Optimization Backend

### Mục tiêu
- Backend tổng hợp cost drivers theo season (expense + payroll + harvest + stock movement).
- Expose API cho AI explanation layer.

### Được phép đọc/sửa
- `module/financial/*`
- `module/season/*` (harvest/payroll context)
- `module/inventory/*` (stock movement cost context)
- `module/sustainability/*`
- `module/ai/*` (nếu cần endpoint bridge)
- migration/test files liên quan

### Không được làm
- Không làm ML pipeline phức tạp.

### Output mong đợi
- Endpoint cost optimization context/summary backend-first.
- Dữ liệu đủ cho AI giải thích tối ưu.

### Verify commands
```powershell
cd agricultural-crop-management-backend
./mvnw -q -DskipTests compile
./mvnw -q test
```

---

## Prompt 6 (P1) — Cost Optimization Frontend

### Mục tiêu
- Kết nối report/expense UI với cost optimization API.
- Thay `AI_TIPS` static bằng dữ liệu backend/AI thật.

### Được phép đọc/sửa
- `src/features/farmer/reports/*`
- `src/features/farmer/expense-management/*`
- `src/entities/expense/*`
- `src/entities/ai/*`
- `src/services/api.farmer.ts` (nếu thêm contract)

### Không được làm
- Không refactor toàn bộ dashboard/report system.

### Output mong đợi
- Widget/screen cost optimization hoạt động với dữ liệu thật.
- Phân tách rõ backend-calculated metrics vs AI explanation.

### Verify commands
```powershell
cd agricultural-crop-management-frontend
npm run typecheck
npm run lint
npm run test -- --run src/features/farmer/reports/components/KPICards.test.tsx src/tests/ExpenseManagement.test.tsx
npm run build
```

---

## Prompt 7 (P2) — Tests / Fixes / Docs Hardening

### Mục tiêu
- Bổ sung test coverage thiếu cho disease + AI + cost.
- Fix contract drift còn lại.
- Cập nhật docs API/usage.

### Được phép đọc/sửa
- Chỉ các file đã sửa ở Prompt 1-6.
- `VERIFY.md` nếu cần bổ sung verify recipes.
- docs liên quan release notes/API notes.

### Output mong đợi
- Test suite ổn định hơn, contract rõ ràng.
- Không còn TODO placeholder cho flow disease/cost mới.

### Verify commands
```powershell
cd agricultural-crop-management-backend
./mvnw -q test

cd ../agricultural-crop-management-frontend
npm run typecheck
npm run lint
npm run test -- --run
npm run build
```

