# COST_OPTIMIZATION_FRONTEND_REPORT_V1

## 1) File da them/sua

### Updated
- `agricultural-crop-management-frontend/src/services/api.farmer.ts`
  - Them schema/type cho cost optimization summary + AI suggestion.
  - Them API functions:
    - `getCostOptimizationSummary(seasonId)`
    - `getCostOptimizationAiSuggestion(seasonId, request?)`
- `agricultural-crop-management-frontend/src/features/farmer/reports/hooks/useReports.ts`
  - Them query lay summary cost optimization theo season.
  - Them mutation goi AI suggestion endpoint.
  - Them state/handler loading-error-suggestion de cap cho UI.
- `agricultural-crop-management-frontend/src/features/farmer/reports/index.tsx`
  - Noi props cost optimization vao `CostTab`.
- `agricultural-crop-management-frontend/src/features/farmer/reports/components/CostTab.tsx`
  - Thay mock chart bang UI cost optimization thuc te.
  - Them budget cards, top categories chart/table, warnings, AI suggestion panel, disclaimer, loading/error state.
- `agricultural-crop-management-frontend/src/features/farmer/reports/types.ts`
  - Them cac type cho cost optimization summary/AI.
- `agricultural-crop-management-frontend/src/features/farmer/season-workspace/SeasonWorkspaceOverview.tsx`
  - Dieu chinh mo ta card Reports de neu ro co cost optimization insight.

## 2) Vi tri UI

- Vi tri chinh: `Season Workspace -> Reports -> tab Cost`
- Route: `/farmer/seasons/:seasonId/workspace/reports`
- Trong tab Cost da co:
  - Card ngan sach mua vu (`Seasonal Budget`)
  - Tong chi phi (`Total Expense`)
  - Ngan sach con lai (`Remaining Budget`)
  - Chi phi tren don vi san luong du kien/thuc te (theo preference weight unit neu co)
  - Top cost categories (bar chart don gian + bang)
  - Warnings list
  - Nut `Phan tich toi uu chi phi bang AI`
  - AI suggestion panel
  - Disclaimer ro rang (AI chi de tham khao)
  - Loading/Error state ro rang cho summary va AI analysis

## 3) API hooks/types da them

### API functions
- Trong `farmerReportsApi`:
  - `getCostOptimizationSummary(seasonId: number)`
  - `getCostOptimizationAiSuggestion(seasonId: number, request?: { question?: string; additionalNote?: string; includeInventory?: boolean })`

### Hook wiring
- Trong `useReports`:
  - Query summary: `useQuery(["farmerReports", "costOptimization", "summary", seasonId], ...)`
  - AI suggestion: `useMutation(...)` goi `getCostOptimizationAiSuggestion`
  - Exposed states:
    - `costOptimizationSummary`
    - `costOptimizationSummaryLoading`
    - `costOptimizationSummaryError`
    - `costOptimizationAiSuggestion`
    - `costOptimizationAiLoading`
    - `costOptimizationAiError`
    - `handleAnalyzeCostOptimizationWithAi`

### Types
- `CostOptimizationCategory`
- `CostOptimizationInventoryUsage`
- `CostOptimizationSummary`
- `CostOptimizationAiSuggestion`

## 4) Cach demo

1. Dang nhap role farmer.
2. Vao 1 season workspace: `/farmer/seasons/{seasonId}/workspace`.
3. Mo module Reports.
4. Chuyen sang tab Cost.
5. Xem cac metric cards, top categories, warnings.
6. Bam `Phan tich toi uu chi phi bang AI`.
7. Kiem tra loading, suggestion text, disclaimer va error state (neu backend loi).

## 5) Lenh verify va ket qua

Thuc thi trong `agricultural-crop-management-frontend`:

1. Typecheck
   - Lenh: `npm run typecheck`
   - Ket qua: `FAILED` (loi pre-existing, ngoai scope cost optimization)
   - Loi chinh:
     - `src/shared/api/marketplace/mock-adapter.ts:910`
     - Kieu `MarketplaceTraceability.lot` thieu `initialQuantity`.

2. Lint
   - Lenh: `npm run lint`
   - Ket qua: `FAILED` (nhieu loi pre-existing toan repo)
   - Tong ket: `328 problems (94 errors, 234 warnings)`.

3. Build
   - Lenh: `npm run build`
   - Ket qua: `SUCCESS`
   - Co warning pre-existing ve chunk size/circular re-export `useAuth`.

## 6) Loi con ton tai

- Typecheck toan frontend dang fail do marketplace traceability type mismatch (khong nam trong scope patch nay).
- Lint toan frontend dang fail do nhieu loi architecture/no-restricted-imports + warnings khac tu code cu.
- Build pass, nhung con warning pre-existing ve circular dependency/chunk split.

## 7) Prompt tiep theo nen gui

```txt
Tiep tuc hoan thien Cost Optimization frontend:
1) Tach API farmer reports khoi legacy `@/services/api.farmer` sang entity layer de pass lint no-restricted-imports.
2) Them unit test cho CostTab (loading/error/success + AI suggestion state).
3) Neu can, bo sung inventory usage section trong Cost tab tu field `inventoryUsageSummary`.
4) Chi sua trong scope reports/season workspace, khong refactor dashboard lon.
```
