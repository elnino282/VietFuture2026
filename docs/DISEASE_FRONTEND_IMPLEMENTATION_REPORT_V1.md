# DISEASE_FRONTEND_IMPLEMENTATION_REPORT_V1

## 1) File da them/sua

### New files
- `agricultural-crop-management-frontend/src/entities/disease/model/schemas.ts`
- `agricultural-crop-management-frontend/src/entities/disease/model/types.ts`
- `agricultural-crop-management-frontend/src/entities/disease/model/keys.ts`
- `agricultural-crop-management-frontend/src/entities/disease/api/client.ts`
- `agricultural-crop-management-frontend/src/entities/disease/api/hooks.ts`
- `agricultural-crop-management-frontend/src/entities/disease/index.ts`
- `agricultural-crop-management-frontend/src/pages/farmer/DiseaseTrackingPage.tsx`

### Updated files
- `agricultural-crop-management-frontend/src/app/routes.tsx`
- `agricultural-crop-management-frontend/src/features/farmer/season-workspace/SeasonWorkspaceLayout.tsx`
- `agricultural-crop-management-frontend/src/features/farmer/season-workspace/SeasonWorkspaceOverview.tsx`

## 2) UI route/tab da them

- Route moi trong Season Workspace:
  - `/farmer/seasons/:seasonId/workspace/disease`
  - Render page: `DiseaseTrackingPage`
- Season workspace tabs:
  - Them tab `Dich benh` (path `disease`) trong `SeasonWorkspaceLayout`.
- Season workspace overview cards:
  - Them card `Disease Tracking` trong `SeasonWorkspaceOverview`.

## 3) API hooks/types da them

### Entity module moi: `entities/disease`
- Schemas/types:
  - Disease record list/filter params
  - Disease record response + detail response
  - Disease treatment response
  - Create/Update request schemas cho record va treatment
  - Enum values: severity/status/effectiveness
- Query key factory:
  - `diseaseKeys.listBySeason(...)`
  - `diseaseKeys.detail(...)`
  - `diseaseKeys.treatmentList(...)`
- API client:
  - `listBySeason`, `getDetail`, `createRecord`, `updateRecord`, `deleteRecord`
  - `listTreatments`, `createTreatment`, `updateTreatment`, `deleteTreatment`
- Hooks:
  - `useDiseaseRecords`
  - `useDiseaseRecordDetail`
  - `useDiseaseTreatments`
  - `useCreateDiseaseRecord`
  - `useUpdateDiseaseRecord`
  - `useDeleteDiseaseRecord`
  - `useCreateDiseaseTreatment`
  - `useUpdateDiseaseTreatment`
  - `useDeleteDiseaseTreatment`

### Query invalidation
- Record create/update/delete:
  - Invalidate list theo season + detail key lien quan.
- Treatment create/update/delete:
  - Invalidate treatments theo record + record detail + disease lists.

## 4) End-to-end user flow

1. Farmer vao Season Workspace:
   - `/farmer/seasons/:seasonId/workspace/disease`
2. Xem danh sach ho so benh theo season:
   - Loc theo `status`, `severity`, `keyword`.
3. Tao ho so benh:
   - Nhap ten benh, trieu chung, muc do, detectedAt, so cay/ty le anh huong, evidenceUrl, ghi chu.
4. Sua/Xoa ho so benh:
   - Thao tac tren tung item.
5. Mo chi tiet ho so benh (expand):
   - Xem thong tin benh + timeline treatment.
6. Them/Sua/Xoa treatment:
   - Nhap treatmentDate, method, supplyItem/supplyLot (neu co), quantity, unit, dosageNote, costAmount, resultNote, effectiveness.
7. Tat ca thao tac hien toast success/fail va refresh du lieu qua query invalidation.
8. Neu season da lock (`COMPLETED/CANCELLED/ARCHIVED`):
   - Disable write actions va hien ly do lock.

## 5) Lenh verify da chay va ket qua

### Typecheck
```bash
npm run typecheck
```
- Ket qua: **FAIL** do loi cu ngoai scope disease.
- File loi: `src/shared/api/marketplace/mock-adapter.ts` (mismatch type `MarketplaceTraceability.lot.initialQuantity`).

### Build
```bash
npx vite build --logLevel error
```
- Ket qua: **PASS**.

### Lint (targeted cho file moi disease)
```bash
npx eslint src/entities/disease src/pages/farmer/DiseaseTrackingPage.tsx
```
- Ket qua: **PASS**.

### Lint (toan repo)
```bash
npm run lint
```
- Ket qua: **FAIL** voi nhieu loi/violation cu (93 errors, 236 warnings), chu yeu debt `no-restricted-imports` (`@/hooks/*`, `@/services/*`) va mot so warning typing.

## 6) Loi con ton tai

1. **Typecheck toan repo** dang fail do loi cu trong marketplace mock adapter, khong lien quan module disease moi.
2. **Lint toan repo** dang fail do debt cu FSD import rules va warning typing tren nhieu module ngoai scope.
3. Build co warning chunk/circular export cu (khong chan build).

## 7) Prompt tiep theo nen gui

1. Viet test frontend cho Disease Tracking:
   - Hook tests (query key + invalidate behavior).
   - Page tests cho list/filter/create/update/delete record va treatment timeline.
2. Neu can, fix typecheck blocker cu trong:
   - `src/shared/api/marketplace/mock-adapter.ts`
   de mo duong cho `npm run typecheck` pass toan repo.
