# AI_DISEASE_SUGGESTION_BACKEND_REPORT_V1

## 1) Endpoint da them

- `POST /api/v1/disease-records/{id}/ai-suggestion`
- Role: `FARMER` (inherit tu `DiseaseRecordController` class-level `@PreAuthorize("hasRole('FARMER')")`)
- Request body (optional):
  - `question` (string, optional)
  - `includeInventory` (boolean, optional; mac dinh `true`)
  - `additionalNote` (string, optional)
- Response (`ApiResponse<DiseaseSuggestionResponse>`):
  - `diseaseRecordId`
  - `suggestionText`
  - `usedContextSummary`
  - `generatedAt`
  - `warning`

## 2) Context duoc dung

Service: `DiseaseSuggestionService`

- Bat buoc:
  - `DiseaseRecord` theo `id`
  - Ownership check qua `Season` + `FarmAccessPort.assertCurrentUserCanAccessSeason(...)`
  - `Season` context: season/plot/crop/variety
  - Treatment history: toi da 10 dong gan nhat
- Bo sung:
  - `FieldLog` gan nhat trong season (toi da 10 dong)
  - `Incident` linked voi disease record (neu co)
  - `Incident` gan nhat trong season (toi da 5 dong) + open incident count
- Inventory context (neu `includeInventory=true`):
  - Lay ton kho duong (`quantity > 0`) trong farm user duoc truy cap
  - Toi da 12 dong inventory de dua vao prompt

## 3) Prompt safety rules

Prompt gui Gemini duoc dong bo cac rang buoc:

- AI chi ho tro quyet dinh tham khao, khong chan doan chac chan.
- Khong duoc dung cum tuong duong voi "chac chan benh la...".
- Khong thay the chuyen gia nong nghiep.
- Khong de xuat hanh dong tu dong tren he thong.
- Khong tu dong tao treatment.
- Khong tu dong tru kho.
- Khong tu dong tao expense.
- Neu thieu du lieu, phai noi ro can bo sung gi.
- Neu khong co inventory noi bo, cam goi y vat tu ngoai du lieu noi bo.
- Neu co inventory, chi goi y vat tu nam trong danh sach ton kho duoc cung cap.
- Bat buoc tra loi theo 6 muc:
  - a) Tom tat tinh trang
  - b) Du lieu con thieu
  - c) Huong xu ly tham khao
  - d) Vat tu hien co co the can nhac
  - e) Rui ro/canh bao
  - f) Buoc tiep theo nen ghi nhan trong he thong
- Bat buoc ket thuc bang disclaimer tham khao.

Ngoai ra backend tra ve truong `warning` de hien thi canh bao an toan phia client.

## 4) File da them/sua

### Them moi
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/ai/dto/request/DiseaseSuggestionRequest.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/ai/dto/response/DiseaseSuggestionResponse.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/ai/service/DiseaseSuggestionService.java`
- `agricultural-crop-management-backend/src/test/java/org/example/QuanLyMuaVu/module/season/controller/DiseaseRecordControllerAiSuggestionTest.java`

### Sua
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/controller/DiseaseRecordController.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/season/repository/FieldLogRepository.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/incident/repository/IncidentRepository.java`
- `agricultural-crop-management-backend/src/main/java/org/example/QuanLyMuaVu/module/inventory/repository/InventoryBalanceRepository.java`

## 5) Lenh verify va ket qua

### Compile
```bash
./mvnw -DskipTests compile
```
Ket qua: `BUILD SUCCESS`.

### Targeted tests
```bash
./mvnw "-Dtest=AiModuleControllerIntegrationTest,DiseaseRecordControllerAiSuggestionTest" test
```
Ket qua: `BUILD SUCCESS`.
- Tong: 3 tests run, 0 failures, 0 errors.

## 6) Loi con ton tai

- Chua co integration/service tests cho `DiseaseSuggestionService` (moi co WebMvc test endpoint).
- Cac endpoint placeholder cu trong `AIController` (`/farmer/ai/suggestions`, `/predict-yield`, `/optimize-cost`) van ton tai, chua duoc thay bang domain-driven flow (ngoai scope prompt nay).
- Console test output van co nhieu warning deprecation `@MockBean` tu test suite cu (khong phat sinh moi trong thay doi nay).

## 7) Prompt tiep theo nen gui

1. Them test cho `DiseaseSuggestionService`:
   - ownership denied
   - includeInventory=false
   - includeInventory=true nhung inventory rong
   - prompt context truncation/safety
2. Neu can, bo sung endpoint docs/schema cho frontend contract va update FE client.
3. Refactor/retire cac AI placeholder endpoint cu trong `AIController` de tranh confusion contract.