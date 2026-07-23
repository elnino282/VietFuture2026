# Báo cáo Tiến độ & Kế hoạch Hoàn thiện — VietFuture2026 (ACM Platform)

---

## 1. Tóm tắt tổng quan

Repo **trưởng thành hơn đáng kể** so với một dự án ở giai đoạn khởi động. Đây không phải là "chưa có backend/frontend" — cả hai đều tồn tại và khá đầy đủ:

- **Backend:** 12 microservices nghiệp vụ (farm, season, crop-catalog, inventory, marketplace, delivery, finance, incident, sustainability, admin-reporting, ai, identity) + api-gateway + shared-config, Spring Boot 3.5, Java 23, MySQL, RabbitMQ, Feign inter-service.
- **Frontend:** React 18 + Vite + Tailwind 4, cấu trúc FSD, 4 portal riêng biệt (Farmer 17 trang, Marketplace/Buyer 30+2 trang, Admin 21 trang, Employee 7 trang), Orval codegen từ OpenAPI.
- Repo đã tự có một tài liệu **BRD/Gap-Analysis nội bộ** rất chi tiết (`docs/BRD_VietFuture2026_Farmer_VietGAP.md`) — tôi đã verify chéo phần lớn nội dung đó với code thực tế thay vì tin nguyên văn, vì tài liệu có thể đã cũ hơn code hiện tại.

**Phát hiện quan trọng nhất:** kể từ khi BRD đó được viết, đội đã **build thêm rất nhiều** — toàn bộ vòng đời audit chứng nhận (Luồng D), đào tạo nhân viên (Luồng K), gate kiểm duyệt Marketplace (Luồng G), và Nhật ký sản xuất hợp nhất (Luồng E) **đã có ở tầng backend**, vượt xa những gì BRD liệt kê là "🆕 chưa có". Nhưng **frontend chưa theo kịp** — đây là rủi ro lớn nhất hiện tại (xem mục 3).

---

## 2. Đối chiếu với 3 trụ cột mục tiêu (theo nội dung buổi họp)

### Trụ cột 1 — Số hóa trọn vòng đời "Nông nghiệp sạch"
| Hạng mục | Trạng thái verify trực tiếp | Bằng chứng |
|---|---|---|
| Vật tư/đầu vào (đất, nước, giống, phân, thuốc BVTV) | ✅ Có | `PesticideRecord`, `NutrientInputEvent`, `IrrigationWaterAnalysis`, `SoilTest`, crop-catalog service |
| Hồ sơ đào tạo nhân sự | ✅ Có (BE đầy đủ, FE một phần) | `TrainingProgram`, `EmployeeTrainingRecord` (season-service) + `UpdateTrainingDialog.tsx` ở FE |
| Cách ly an toàn hóa chất trước thu hoạch (PHI) | ✅ Có, có unit test | `PHIHarvestValidationService`, `PesticidePHIReference`, `PHIHarvestValidationServiceTest` |
| Phân loại sau thu hoạch (đạt chuẩn / cho chăn nuôi / phân bón / hủy) | ✅ Có | enum `SubStandardDisposition{SELL_LIVESTOCK_FEED, COMPOSTING, PROCESSING, DISCARDED, SELL_DISCOUNT}` |
| Kho lạnh & cảnh báo nhiệt độ | ✅ Có | `ColdChainValidationService`, `Warehouse.temperatureMin/Max`, `hasTemperatureAlert` |
| Bug chặn build từng ghi nhận (`Task.java` trùng field, thiếu import) | ✅ Đã fix | Verify trực tiếp: `plotName` chỉ còn 1 khai báo, đã có `import java.math.BigDecimal` |

**Kết luận:** trụ cột 1 coi như hoàn chỉnh ở tầng backend. Rủi ro còn lại chủ yếu ở FE (xem checklist §2.6 trong BRD gốc — chưa re-verify UI thực tế trong lượt audit này, nên coi là "cần QA" chứ không phải "đã xong").

### Trụ cột 2 — Tự động hóa hồ sơ VietGAP/Hữu cơ
| Hạng mục | Backend | Frontend |
|---|---|---|
| Nhật ký sản xuất hợp nhất theo timeline | ✅ `ProductionDiaryController` + `ProductionDiaryAggregationService` | ❌ **Không tìm thấy trang/component nào tiêu thụ API này** ngoài type generated |
| Tự sinh hồ sơ / xuất bundle hồ sơ (export dossier) | ✅ `POST /export-dossier` gọi ngược sang production-diary rồi tạo `FarmDocument` | ❌ Chưa thấy nút/màn hình gọi endpoint này |
| Vòng đời audit ngoài (lịch hẹn → audit → kết luận) | ✅ Đầy đủ CRUD lifecycle (`CertificationAuditController`) | ❌ **0 file non-generated** tham chiếu |
| Biên bản không phù hợp (nonconformity) | ✅ Có entity + endpoint ghi nhận/xem | ❌ Chưa có UI |
| Kế hoạch khắc phục (corrective action: tạo → sửa → nộp → auditor duyệt) | ✅ Đầy đủ 4 bước | ❌ Chưa có UI |
| Cấp giấy chứng nhận + Admin duyệt tài liệu → tự động publish | ✅ Có | Chưa verify |
| Đa tiêu chuẩn (VietGAP / Hữu cơ) | 🔧 `CertificationStandard.type` hỗ trợ nhiều loại nhưng `getOrCreateRecord()` vẫn hard-code `"VIETGAP-PLANTING-2024"` | — |
| Test tự động cho toàn bộ luồng audit/nonconformity/corrective action | ❌ **Không tìm thấy file test nào** | — |

**Kết luận:** đây là phần công sức lớn nhất và cũng là phần **rủi ro cao nhất**. Backend đã xây gần như trọn vẹn đúng yêu cầu cốt lõi mà stakeholder nhấn mạnh nhất trong buổi họp ("*collect ra hồ sơ để hỗ trợ người ta đăng ký*"), nhưng **người dùng cuối (farmer/auditor/admin) chưa thể thao tác được** vì thiếu toàn bộ giao diện. Ngoài ra chưa có test tự động bảo vệ, nên mọi thay đổi tiếp theo dễ gây regression âm thầm.

### Trụ cột 3 — Minh bạch truy xuất & thương mại
| Hạng mục | Trạng thái |
|---|---|
| Truy xuất nguồn gốc công khai (timeline gieo trồng → thu hoạch) | ✅ Có (`MarketplaceTraceabilityResponse`, `ProductWarehouseTraceabilityResponse`) |
| Ẩn dữ liệu chi phí nội bộ khỏi truy xuất công khai | ✅ Verify: không có field cost/price/expense nào trong 2 response trên |
| Gate kiểm duyệt: chặn publish sản phẩm nếu chứng nhận hết hạn / không đạt PHI | ✅ Đã implement thật (không còn là lỗ hổng như BRD ghi) — `complianceGateService.checkCompliance()` chặn `updateAdminProductStatus` khi set `PUBLISHED` |
| Cấu hình phí ship linh hoạt + phụ phí lạnh | ✅ Có (`DeliveryRate`, `ShippingFeeCalculator`) |
| Đặt trước (pre-order) hẹn ngày giao | 🔧 Có field cơ bản (`isPreOrder`, `requestedDeliveryDate`, validate bắt buộc nhập ngày khi pre-order) — **nhưng 0 dấu vết ở frontend** (buyer không có cách chọn pre-order) |
| Gom đơn theo khu vực để giảm phí ship | ❌ Chưa có — không tìm thấy logic consolidation/batch nào |
| Same-day fulfillment queue cho farmer | 🔧 Backend hỗ trợ rate `same_day`; chưa verify tab lọc ở FE farmer |

---

## 3. Rủi ro & nợ kỹ thuật cần lưu ý

1. **FE tụt hậu so với BE ở đúng phần trọng tâm nhất** (nhật ký sản xuất hợp nhất + vòng đời audit chứng nhận) — đây là phần mà stakeholder nhấn mạnh là giá trị cốt lõi của sản phẩm, nhưng hiện tại **demo cho khách/giảng viên sẽ không có gì để bấm xem**.
2. **Không có test tự động** cho toàn bộ cụm tính năng audit/nonconformity/corrective-action mới xây — rủi ro regression cao khi tiếp tục phát triển.
3. **Hard-code `"VIETGAP-PLANTING-2024"`** trong `getOrCreateRecord()` — chặn khả năng mở rộng sang chứng nhận Hữu cơ dù model đã hỗ trợ đa tiêu chuẩn.
4. **Thuật toán gom đơn theo khu vực chưa tồn tại** — cần quyết định nghiệp vụ (ngưỡng số lượng/khu vực, thời gian chờ gom đơn) trước khi code, không phải việc auto-implement được ngay.
5. Không build được backend trong môi trường audit này → **chưa xác nhận 12 service compile sạch ở trạng thái hiện tại** dù bug P0 cũ đã fix. Cần CI xanh làm bằng chứng cuối.

---

## 4. Kế hoạch triển khai theo Phase

### Phase 1 — Đóng gap Frontend cho phần đã có Backend (ưu tiên cao nhất, "low-hanging fruit" về logic nhưng critical về giá trị demo)
- Xây trang **"Nhật ký sản xuất"** hợp nhất cho Farmer: gọi `GET /farmer/seasons/{seasonId}/production-diary`, hiển thị timeline (phân bón, thuốc BVTV, tưới nước, đất, sơ chế).
- Thêm nút **"Xuất hồ sơ đăng ký chứng nhận"** gọi `POST /export-dossier`, cho tải về/preview `FarmDocument` sinh ra.
- Xây cụm màn hình **vòng đời Audit VietGAP**: Farmer xem danh sách nonconformity → tạo/sửa/nộp corrective action; Admin/Auditor lên lịch audit, bắt đầu, kết luận, ghi nonconformity, duyệt corrective action, cấp giấy chứng nhận.
- Màn hình Admin duyệt tài liệu (`verifyDocument`) nếu chưa có UI tương ứng.
- QA lại checklist cũ trong BRD (progress bar diện tích, binding `plotName`/`plotArea` ở Employee Portal, widget PHI blocking) — vì đây là mã có sẵn nhưng chưa re-verify UI thật trong lượt audit này.

### Phase 2 — Hoàn thiện Thương mại & Logistics
- FE cho pre-order: toggle "Đặt trước" + date picker `requestedDeliveryDate` ở checkout; FE farmer fulfillment queue lọc theo same-day/pre-order.
- Thiết kế và implement thuật toán **gom đơn theo khu vực** (`[CẦN XÁC NHẬN]` — cần chốt quy tắc nghiệp vụ với Product Owner trước: ngưỡng thời gian chờ, phạm vi khu vực, cách tính phí sau khi gom).
- Tham số hóa `standardCode` (bỏ hard-code) để mở khóa luồng chứng nhận Hữu cơ song song VietGAP.
- Bổ sung reference/config đóng gói-sơ chế theo từng nhóm cây trồng (rau/hoa quả vs ngũ cốc).

### Phase 3 — Vận hành dài hạn & Tái kiểm định
- Cơ chế nhắc tái kiểm định định kỳ cho state `PERIODIC_REVIEW_DUE` (đã có trong enum nhưng cần scheduler + notification thật).
- API/dashboard tổng hợp **completeness score** và **missing evidence** cho Admin (tái dùng `CertificationScoringService`, không tạo bảng điểm song song).
- Nối kết quả AI chẩn đoán sâu bệnh (`GeminiService`) thành gợi ý `PesticideRecord` nháp ngay trong màn Nhật ký sản xuất.

### Phase 4 — Chất lượng & Production-readiness
- Viết test tự động (unit + integration) cho toàn bộ cụm audit/nonconformity/corrective-action — hiện đang là **0%** coverage.
- Xác nhận CI backend xanh trên cả 12 service (chạy `mvn clean verify` thật, không chỉ static review).
- Mở rộng Prometheus dashboard/alert cho các luồng mới (audit, pre-order, dossier export).
- Load-test marketplace + inventory cho kịch bản đồng thời nhiều buyer/farmer thao tác cold-chain alert.

---

*Báo cáo này dựa trên audit tĩnh mã nguồn — không thay thế cho việc chạy thử end-to-end. Khuyến nghị dùng Phase 1 làm mục tiêu sprint kế tiếp vì đây là khoảng cách lớn nhất giữa "đã code" và "khách hàng nhìn thấy được".*
