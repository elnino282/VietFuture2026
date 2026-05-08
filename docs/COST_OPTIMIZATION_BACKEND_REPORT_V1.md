# COST_OPTIMIZATION_BACKEND_REPORT_V1

## 1) Endpoint da them

- `GET /api/v1/seasons/{seasonId}/cost-optimization/summary`
  - Tra tong hop chi phi mua vu da tinh san tu backend.
- `POST /api/v1/seasons/{seasonId}/cost-optimization/ai-suggestion`
  - Nhan request optional:
    - `question`
    - `additionalNote`
    - `includeInventory`
  - Tra ve summary + warning + goi y AI + disclaimer.

Controller: `SeasonCostOptimizationController`

## 2) Cong thuc / tong hop da dung

Service: `SeasonCostOptimizationService`

- Kiem tra quyen:
  - Load `Season` theo `seasonId`
  - `farmAccessService.assertCurrentUserCanAccessSeason(season)`
- `budgetAmount`:
  - Lay tu `season.budgetAmount` (null -> `0`)
- `totalExpense`:
  - Tong `expense.getEffectiveAmount()` cua tat ca expense theo season
  - `getEffectiveAmount()`: uu tien `amount`, fallback `totalCost`, fallback `unitPrice * quantity`
- `remainingBudget`:
  - `budgetAmount - totalExpense`
- `expenseByCategory`:
  - Group theo `category` (null/blank -> `UNCATEGORIZED`)
  - Tinh `amount` va `%` tren tong chi phi
- `topCostCategories`:
  - Sap xep giam dan theo `amount`, lay top 3
- `expectedYieldKg`:
  - Tu `season.expectedYieldKg` (chi nhan gia tri > 0)
- `actualYieldKg`:
  - Uu tien `season.actualYieldKg` (>0)
  - Fallback `SUM(harvest.quantity)` theo season neu season khong co actual yield
- `costPerExpectedKg`:
  - `totalExpense / expectedYieldKg` (null neu thieu/mau = 0)
- `costPerActualKg`:
  - `totalExpense / actualYieldKg` (null neu thieu/mau = 0)
- `laborCost`:
  - `SUM(payroll_records.total_amount theo season) + SUM(expense labor-like category)`
  - Labor-like category: co chua `LABOR|LABOUR|PAYROLL|NHAN_CONG`
- `pesticideTreatmentCost`:
  - Tu `disease_treatments` cua season
  - Uu tien `costAmount`
  - Neu `costAmount` null va co `expense` link -> lay `expense.getEffectiveAmount()`
  - Co de-dup theo `expenseId` de tranh cong lap khi fallback tu expense
- `inventoryUsageSummary`:
  - Tong hop `stock_movements` theo season
  - Chi tinh movement type `OUT`
  - Group theo `(itemName, unit)` -> `totalOutQuantity`, `movementCount`
  - Lay toi da 12 dong

## 3) Rule canh bao

- Vuot ngan sach:
  - `totalExpense > budgetAmount` (khi budget > 0)
- Ngan sach con lai thap:
  - `remainingBudget <= 10% * budgetAmount` (va >= 0)
- Category chiem ty trong cao:
  - Top category co `% >= 40%` tong chi phi
- Chi phi thuoc/dieu tri cao:
  - `pesticideTreatmentCost / totalExpense >= 25%`
- Thieu expected yield:
  - `expectedYieldKg` null/<=0 -> khong tinh duoc `costPerExpectedKg`
- Them canh bao khi budget khong hop le:
  - `budgetAmount <= 0`

## 4) Prompt AI

AI suggestion endpoint dung du lieu tong hop tu backend, prompt co rang buoc:

- AI chi duoc giai thich tren so lieu backend da tinh san (khong tu tinh so lieu goc).
- Format tra loi bat buoc:
  - a) Tom tat buc tranh chi phi mua vu
  - b) Du lieu con thieu
  - c) Huong toi uu chi phi tham khao
  - d) Vat tu su dung hien co co the can nhac
  - e) Rui ro/canh bao
  - f) Buoc tiep theo nen ghi nhan tren he thong
- Safety rules:
  - AI chi ho tro quyet dinh.
  - Khong thay the tu van tai chinh/chuyen gia nong nghiep.
  - Khong tu dong sua expense/budget/inventory.
  - Khong tu dong quyet dinh mua vat tu.
  - Khong khang dinh loi nhuan neu thieu revenue.
  - Neu thieu du lieu phai noi ro thong tin can bo sung.
- Co dong disclaimer bat buoc o cuoi cau tra loi.

## 5) File da them / sua

### Added
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/controller/SeasonCostOptimizationController.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/ai/service/SeasonCostOptimizationService.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/ai/dto/request/SeasonCostOptimizationSuggestionRequest.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/ai/dto/response/SeasonCostOptimizationSummaryResponse.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/ai/dto/response/SeasonCostOptimizationSuggestionResponse.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/ai/dto/response/SeasonCostCategoryBreakdown.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/ai/dto/response/SeasonInventoryUsageSummary.java`
- `agricultural-crop-management-backend/src/test/java/org/example/QuanLyMuaVu/module/season/controller/SeasonCostOptimizationControllerTest.java`
- `docs/COST_OPTIMIZATION_BACKEND_REPORT_V1.md`

### Updated
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/repository/PayrollRecordRepository.java`
  - Them `sumTotalAmountBySeasonId`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/repository/DiseaseTreatmentRepository.java`
  - Them `findAllBySeasonIdWithExpense`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/inventory/repository/StockMovementRepository.java`
  - Them `findAllBySeasonIdWithLotAndItem`

## 6) Lenh verify va ket qua

Thuc thi trong `agricultural-crop-management-backend`:

1. Compile:
   - `./mvnw -q -DskipTests compile`
   - Ket qua: `SUCCESS`
2. Test targeted:
   - `./mvnw -q "-Dtest=SeasonCostOptimizationControllerTest,DiseaseRecordControllerAiSuggestionTest" test`
   - Ket qua: `SUCCESS`

## 7) Loi con ton tai

- Khong thay loi compile/test blocker trong pham vi backend cost optimization vua implement.
- Luu y workspace hien tai dang co nhieu thay doi/untracked tu cac task truoc (khong nam trong scope patch nay).

## 8) Prompt tiep theo nen gui

```
Dua tren COST_OPTIMIZATION_BACKEND_REPORT_V1.md, hay implement frontend UI cho cost optimization trong Season Workspace:
- Them tab/card "Cost Optimization"
- Goi GET /api/v1/seasons/{seasonId}/cost-optimization/summary
- Goi POST /api/v1/seasons/{seasonId}/cost-optimization/ai-suggestion
- Hien thi summary metrics + warning list + ai suggestion panel
- Co loading/error state ro rang
- Hien thi disclaimer an toan AI
- Khong tu dong sua expense/inventory
- Chay typecheck/build frontend va tao COST_OPTIMIZATION_FRONTEND_REPORT_V1.md
```
