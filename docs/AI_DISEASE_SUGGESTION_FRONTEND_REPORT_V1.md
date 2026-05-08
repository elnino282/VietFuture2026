# AI_DISEASE_SUGGESTION_FRONTEND_REPORT_V1

## 1) File da them/sua

### Files sua
- `agricultural-crop-management-frontend/src/entities/disease/model/schemas.ts`
- `agricultural-crop-management-frontend/src/entities/disease/model/types.ts`
- `agricultural-crop-management-frontend/src/entities/disease/api/client.ts`
- `agricultural-crop-management-frontend/src/entities/disease/api/hooks.ts`
- `agricultural-crop-management-frontend/src/entities/disease/index.ts`
- `agricultural-crop-management-frontend/src/pages/farmer/DiseaseTrackingPage.tsx`

### File bao cao moi
- `docs/AI_DISEASE_SUGGESTION_FRONTEND_REPORT_V1.md`

## 2) Vi tri UI nut AI

Trang: `DiseaseTrackingPage` (Season Workspace > Disease Tracking).

- Trong phan chi tiet cua tung disease record (khi expand), da them panel:
  - Tieu de: `Goi y xu ly bang AI`
  - Nut action: `Goi y xu ly bang AI`
  - Additional note (optional)
  - Toggle `Bao gom context ton kho`
  - Error panel neu goi API/Gemini loi
  - Ket qua goi y (text) + warning/disclaimer

Vi tri code chinh:
- `DiseaseTrackingPage.tsx`:
  - Hook AI state/mutation: quanh dong `432+`
  - Handler goi AI: quanh dong `568+`
  - UI panel AI trong detail: quanh dong `952+`

## 3) API hook da them

### Schema/type moi
- `DiseaseSuggestionRequestSchema`
- `DiseaseSuggestionResponseSchema`
- `DiseaseSuggestionRequest` / `DiseaseSuggestionResponse`

### API client
- Ham moi: `diseaseApi.requestAiSuggestion(diseaseRecordId, data?)`
- Endpoint: `POST /api/v1/disease-records/{id}/ai-suggestion`

### React Query hook
- Hook moi: `useDiseaseAiSuggestion()` (mutation)
- Khong invalidate query (read-only suggestion flow).

## 4) Cach demo end-to-end

1. Chay backend co endpoint AI da implement.
2. Chay frontend va vao route:
   - `/farmer/seasons/:seasonId/workspace/disease`
3. Mo mot disease record bang nut `Xem chi tiet`.
4. Trong panel `Goi y xu ly bang AI`:
   - (tuy chon) nhap `Additional note`
   - bat/tat `Bao gom context ton kho`
   - bam `Goi y xu ly bang AI`
5. Kiem tra:
   - luc dang goi: button loading (spinner)
   - neu loi: hien error panel
   - neu thanh cong: hien `suggestionText`, `generatedAt`, `warning`
   - luon hien disclaimer tham khao: `Gợi ý chỉ mang tính tham khảo, không thay thế tư vấn chuyên gia.`
6. Xac nhan khong co hanh dong tu dong tao treatment/tru kho/tao expense.

## 5) Lenh verify va ket qua

### Typecheck
```bash
npm run typecheck
```
Ket qua: **FAIL** do loi cu ngoai scope disease AI:
- `src/shared/api/marketplace/mock-adapter.ts` (TS2322, missing `lot.initialQuantity`).

### Build
```bash
npx vite build --logLevel error
```
Ket qua: **PASS**.

### Lint targeted scope
```bash
npx eslint src/entities/disease src/pages/farmer/DiseaseTrackingPage.tsx
```
Ket qua: **PASS**.

### Lint toan repo
```bash
npm run lint
```
Ket qua: **FAIL** voi debt cu (93 errors, 236 warnings), chu yeu `no-restricted-imports` va mot so warning typing ngoai scope.

## 6) Loi con ton tai

1. `npm run typecheck` toan repo van fail do loi marketplace mock adapter (khong lien quan thay doi nay).
2. `npm run lint` toan repo van fail do debt cu FSD import restrictions/typing (khong lien quan thay doi nay).
3. Chua co frontend test tu dong rieng cho AI suggestion panel (chi verify bang build/lint + thao tac thu cong).

## 7) Prompt tiep theo nen gui

1. Them test cho `DiseaseTrackingPage` AI panel:
   - success state
   - error state
   - loading state
   - assert disclaimer hien thi.
2. Neu can, bo sung parser hien thi `usedContextSummary` de debug context da gui (chi read-only).
3. Fix blocker typecheck ngoai scope tai marketplace mock adapter de mo duong CI typecheck full repo.
