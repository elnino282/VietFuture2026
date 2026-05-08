# DISEASE_AI_GAP_ANALYSIS_V1

## 1) Gap summary
- Core gap lớn nhất: dữ liệu bệnh và điều trị hiện đang rải ở `FieldLog.notes` / `Incident.description` dạng text, chưa có mô hình cấu trúc để truy vấn, thống kê, và feed AI theo ngữ cảnh chuẩn.
- Core gap thứ hai: AI endpoint disease/cost đang placeholder hoặc contract lệch frontend.
- Core gap thứ ba: upload bằng chứng ảnh cho disease chưa có pipeline chuyên dụng ở domain mùa vụ.

## 2) Requirement mapping table (low-risk implementation oriented)

| Yêu cầu | Module/file hiện có | Thiếu gì | Bổ sung ít rủi ro nhất |
|---|---|---|---|
| Farmer ghi lịch sử cây bị bệnh theo mùa vụ | `module/season` + `module/incident` + `field_logs` table | Chưa có entity disease record riêng theo season/plot/crop context | Tạo `DiseaseRecord` (season-scoped) + repository/service/controller mới, chỉ link nhẹ sang `Season`, optional `FieldLog`, optional `Incident` |
| Ghi triệu chứng, mức độ, số cây/diện tích ảnh hưởng, thời điểm phát hiện | `FieldLog` có `logDate/logType/notes`; `Incident` có `severity` | Thiếu trường cấu trúc symptom list + affectedPlantCount + affectedArea + discoveredAt | Thêm bảng/DTO structured (không nhồi thêm cột vào `field_logs` text-first) |
| Ghi lịch sử điều trị nhiều lần cho một ca bệnh | `Incident` status flow có thể resolve + resolutionNote text | Không có child history many-to-one cho treatment steps | Tạo `DiseaseTreatment` child của `DiseaseRecord` (method, supplyLot/item, doseQty, costAmount, resultNote, treatedAt) |
| Tận dụng vật tư hiện có trong hệ thống | `SupplyItem/SupplyLot/InventoryBalance/StockMovement` đã đủ giàu ngữ cảnh | Chưa có quan hệ disease->supply usage | Trong `DiseaseTreatment`, lưu `supplyLotId` (ưu tiên), fallback `supplyItemId` + snapshot tên/đơn vị tại thời điểm ghi nhận |
| Ghi nhận chi phí điều trị | `Expense` có season/task/category/amount | Chưa có disease link | Thêm optional `diseaseRecordId` hoặc `diseaseTreatmentId` vào expense (phase sau), hoặc tạo expense theo category `PEST_DISEASE` + reference field |
| Ảnh minh chứng bệnh | Chỉ thấy upload file ở marketplace payment proof; documents chỉ lưu URL | Không có endpoint upload disease evidence trong farmer season domain | P0 dùng URL-based evidence list (nếu cần nhanh), P1 thêm upload endpoint domain disease (multipart + storage path tách biệt) |
| AI gợi ý dựa trên lịch sử bệnh + kho vật tư | `GeminiService` + `ChatController` có thật; `AIController` placeholder | Thiếu aggregator context và prompt contract disease-specific | Thêm service tổng hợp context (`DiseaseContextAssembler`) + endpoint read-only `/farmer/ai/disease-suggestions` |
| AI không tự sửa dữ liệu nghiệp vụ | Kiến trúc hiện tại chưa có write-call từ AI | Chưa có guardrail contract rõ trong endpoint | Response-only DTO; service chỉ đọc; controller không gọi command service; audit prompt-level disclaimer bắt buộc |
| AI ưu tiên vật tư có sẵn, không bịa thuốc | Inventory query đầy đủ | Chưa có ranking/filter logic “in-stock first” | Context builder phân loại: in-stock / low-stock / out-of-stock; prompt chỉ cho phép đề xuất từ danh sách này |
| Tối ưu chi phí mùa vụ backend-first | `FarmerReportService` đã tính cost/revenue/profit | Chưa có API “cost optimization context” cho AI | Thêm endpoint backend tổng hợp cost drivers theo season (expense + payroll + stock out + harvest efficiency) |
| Frontend workspace disease tracking | `FieldLogsPage`, `IncidentsPage`, `SeasonWorkspaceLayout` | Form/DTO chưa có disease structured fields, chưa có treatment timeline UI | Thêm disease tab/section ở workspace, form wizard: record + treatments + evidence |
| Frontend AI disease/cost surfaces | `AiAssistantPage` + `entities/ai` + `AIOptimizationTips` | Data source rỗng/placeholder; contract mismatch với backend | Align schemas/client theo endpoint mới, render recommendations + confidence + disclaimer |

## 3) Incident hiện tại vs DiseaseRecord/DiseaseTreatment riêng

### Option A: Dùng Incident hiện tại làm disease model chính
**Ưu điểm**
- Ít bảng mới.
- Tận dụng status/severity/deadline có sẵn.

**Nhược điểm**
- `Incident.description` sẽ bị quá tải text, khó query treatment history.
- Khó biểu diễn nhiều lần điều trị / nhiều vật tư / liều lượng theo thời gian.
- Mở rộng AI context và thống kê cost-by-disease sẽ nhiều logic parse text, rủi ro cao.

### Option B: Tạo `DiseaseRecord` + `DiseaseTreatment` riêng, Incident chỉ là lớp cảnh báo/ticket
**Ưu điểm**
- Domain rõ, dữ liệu chuẩn hóa cho AI + report + truy vết điều trị.
- Dễ map với inventory/expense một cách typed.
- Giảm rủi ro regression cho luồng Incident generic đang dùng ở nơi khác.

**Nhược điểm**
- Cần migration + API + UI mới.
- Cần chiến lược liên kết mềm với incident/fieldlog cũ.

### Recommendation
- **Khuyến nghị chọn Option B**.
- Giữ `Incident` cho ticket/alert workflow; thêm `diseaseRecordId` optional trong incident nếu muốn liên kết.
- Không refactor phá vỡ Incident hiện tại trong P0/P1.

## 4) Low-risk sequencing note
- P0 tập trung disease data model + CRUD + UI ghi nhận cơ bản.
- P1 mới thêm AI recommendations và cost optimization explanation.
- P2 gom test/contract hardening + docs finalize.

