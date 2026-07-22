Nền tảng Quản lý Nông nghiệp VietFuture2026 — Giai đoạn 3 & 4
Kế hoạch triển khai chi tiết dựa trên phân tích kiến trúc microservices hiện tại (12 services, React frontend, Spring Boot, RabbitMQ, MySQL)

Tổng quan Kiến trúc Hiện tại
Project đã có nền tảng vững chắc: Farm Service (farm_db), Season Service (season_db), Inventory Service (inventory_db), Marketplace Service (marketplace_db), Sustainability Service (sustainability_db) — mỗi service database riêng, giao tiếp qua RabbitMQ. Marketplace đã có traceability chain cơ bản (Farm → Plot → Season → Harvest → ProductLot) nhưng thiếu: chứng nhận VietGAP, tính toán PHI, quản lý tài liệu nông trại, và giao hàng logistics.

4 Modules
Giai đoạn 3
2 Modules
Giai đoạn 4
3-4
Services affected
16 tuần
Timeline
GIAI ĐOẠN 3: Hệ thống Tiêu chuẩn & Truy xuất nguồn gốc
Module 3.1 — Quản lý Tài liệu Nông trại (Farm Document Management)
Nghiệp vụ thực tế (theo VietGAP TCVN 11892:2017)

Mỗi nông trại cần lưu trữ: Giấy phép đất, sơ đồ mặt bằng, hồ sơ nhân viên, nhật ký phân bón/thuốc trừ sâu, báo cáo kiểm tra nội bộ, giấy chứng nhận nguồn nước, hồ sơ thu hoạch. Đây là yêu cầu bắt buộc để được cấp chứng nhận VietGAP.

Tích hợp vào codebase hiện tại

Tạo entity FarmDocument trong Farm Service (farm_db), chung database với Farm và Plot. Tái sử dụng MinIO storage đã có sẵn trong Marketplace Service cho file upload.

Chi tiết kỹ thuật
Entity fields: farm_id, document_type (enum: LAND_CERTIFICATE, SOIL_TEST_REPORT, WATER_TEST_REPORT, PESTICIDE_RECORD, FERTILIZER_RECORD, HARVEST_LOG, INTERNAL_AUDIT, CERTIFICATE, OTHER), title, file_url (MinIO), issued_date, expiry_date, verification_status.
API: POST/GET/PUT/DELETE /api/v1/farms/{farmId}/documents
UI: FarmDocumentsPage.tsx với upload file, phân loại theo danh mục VietGAP, notification khi tài liệu sắp hết hạn.
Module 3.2 — Chứng minh Đạt chuẩn VietGAP (Certification Engine — Auto-compliance checking)
4 nhóm tiêu chí VietGAP trồng trọt cần implement

Tính năng cốt lõi

— Certification Checklist Engine: Entity CertificationStandard lưu checklist theo loại cert (VietGAP_Planting, Organic, GlobalGAP). Entity CertificationRecord lưu compliance status của từng farm.
— Auto-populate từ dữ liệu hiện có: Khi farmer đã ghi Field Logs, Soil Tests, Water Tests, Disease Records → certification engine tự động mapping sang checklist items. Ví dụ: soil_test.pH > 5.5 → checklist item "pH đất ≥ 5.5" = PASS.
— Compliance Score Dashboard: Widget hiển thị % compliance, checklist với trạng thái PASS/FAIL/PENDING. Khi compliance ≥ 80% → enable "Apply for Certificate" button.

Chi tiết kỹ thuật
Entity: CertificationStandard, CertificationRecord, ChecklistItem. API: /api/v1/certifications/* (Farm Service). UI: CertificationPage.tsx với dashboard compliance score. Admin duyệt chứng nhận.
Module 3.3 — Tính toán Số ngày Cách ly An toàn (PHI Calculator)
Nghiệp vụ thực tế

PHI (Pre-Harvest Interval) là số ngày tối thiểu từ khi phun thuốc BVTV đến khi thu hoạch. VietGAP yêu cầu ghi chép đầy đủ ngày phun, loại thuốc, PHI, và ngày thu hoạch để đảm bảo dư lượng thuốc dưới MRL (Maximum Residue Limit). PHI được tính = application_date + phi_days.

Business logic quan trọng nhất

CRITICAL: Harvest Safety Validation
Khi farmer tạo Harvest record → hệ thống tự động check tất cả PesticideRecord của season đó. Nếu harvest_date < harvest_allowed_date của bất kỳ pesticide nào → BLOCK harvest, hiển thị cảnh báo: "Thu hoạch chưa được phép. Thuốc [tên] cần cách ly đến ngày [date]."
Chi tiết kỹ thuật
Entity: PesticideRecord (season_db) — pesticide_name, application_date, phi_days, harvest_allowed_date. Reference table: PesticidePHIReference với ~200+ loại thuốc phổ biến VN (Carbendazim, Chlorpyrifos, Cypermethrin...).
UI: PesticideLogPage.tsx, widget "Harvest Safety Status" trên mùa vụ dashboard (màu xanh/vàng/đỏ).
Module 3.4 — Minh bạch với Người tiêu dùng (Enhanced Public Traceability — QR Code & Public Trace Page)
Nghiệp vụ thực tế

Người mua quét QR code trên sản phẩm → xem toàn bộ hành trình từ nông trại đến tay mình. Thông tin public gồm: tên nông trại, chứng nhận, ngày gieo/trồng, thu hoạch, phân bón/thuốc đã dùng (an toàn/chưa an toàn), thông tin kho bãi.

Nâng cấp từ codebase hiện tại

Marketplace đã có MarketplaceTraceabilityResponse với Farm, Plot, Season, Harvest, ProductLot, Timeline milestones. Cần mở rộng:
— Thêm CertificationBadge: cert name, issued_date, expiry, status.
— Thêm PesticideSummary: danh sách thuốc đã dùng với PHI status (SAFE/CAUTION/BLOCKED).
— Endpoint công khai: GET /api/v1/marketplace/products/{productId}/public-trace (không cần auth).
— QR Code generation: encode public-trace URL.

Chi tiết kỹ thuật
Public trace page tại /trace/{productId} — hoàn toàn public, responsive mobile, timeline với icons, certification badge, safety status, farm info với map location.
GIAI ĐOẠN 4: UI/UX & Vận chuyển (Logistics)
Module 4.1 — Tối ưu hiển thị (UI/UX Optimization) — Dashboard KPIs + Search Window Pattern
Tối ưu Farmer Dashboard

KPI cards ngay trên đầu: sản lượng mùa vụ hiện tại, tổng chi phí, đơn hàng chờ xử lý, compliance score. Thêm widget "Quick Actions" với buttons cho task thường dùng. Charts inline thay vì mở trang mới.

Search Window Pattern

Hiện tại ProductListPage dùng URL search params → kết quả trên trang mới. Thêm SearchWindow component (Dialog-based overlay) — tham khảo pattern từ ImageSearchModal.tsx: Cmd+K để mở, ESC để đóng, kết quả có thể navigate hoặc add-to-cart inline. DebouncedInput với TanStack Query cho search không gây lag.

QuickSearchBar ở Header

Component QuickSearchBar trong farmer workspace header — tìm kiếm nhanh tasks, seasons, products, documents. Fuzzy search với TanStack Query.

Module 4.2 — Tích hợp Giao hàng Nhanh (Fast Delivery Integration — Cold Chain & Dynamic Shipping)
Nghiệp vụ thực tế (Vietnamese fresh produce logistics)

Nông sản tươi dễ hư hỏng cần giao trong 24-48h. Chi phí ship là rào cản lớn — người mua chấp nhận tối đa 15-25% giá trị đơn hàng. Các đơn vị: GHTK, GHN, Ninja Van, J&T Express. Tính phí dựa trên: khoảng cách, cân nặng, khu vực, dịch vụ đặc biệt (cold chain).

Architecture

Tạo Delivery Service (mới)

Port 8092, database delivery_db. Entity: DeliveryOrder, DeliveryProvider (GHTK/GHN/NINJA/JT), DeliveryRate, DeliveryStatus enum.

Shipping Fee Calculator

Algorithm: base_rate + (distance × weight × zone_multiplier). Demo mode với static rates trước khi integrate real APIs.

Cold Chain Support

DeliveryRequirement: is_perishable, requires_cold_chain, max_hours, temp_min/max. Tự động filter providers hỗ trợ cold chain.

Order Batching

Gom đơn từ nhiều farm trong cùng tỉnh/thành phố → suggest same-day delivery để giảm phí ship.

Chi tiết kỹ thuật
Entity: DeliveryOrder, DeliveryProvider, DeliveryRate, DeliveryStatus, DeliveryRequirement. API: /api/v1/delivery/* — calculate, create, track. Frontend: checkout page với delivery options (standard/express/same-day), order detail với tracking timeline. Farmer seller portal với delivery orders tab.
Chiến lược Triển khai — Tránh Xung đột với Code Hiện tại
1. Additive Only

Tất cả thay đổi là additive — không sửa đổi entity/service hiện có. Migration files mới (V4__, V5__...) không thay đổi bảng cũ. Không sửa API contracts đã public của marketplace.

2. Domain Boundary rõ ràng

Farm Document → Farm Service | Certification → Farm + Sustainability | PHI → Season Service | Delivery → Delivery Service (mới) | Public Trace → Marketplace.

3. Event-Driven Extension

Season Service phát event HarvestRecorded → PHI Calculator lắng nghe validate. Farm Service phát event FarmDocumentUploaded → Certification Engine check compliance. Admin Reporting subscribe events mới để update read models.

4. Feature Flags & Rollback

Config flag enable/disable features mới. Mỗi migration có rollback script. Flyway migrate:undo theo thứ tự ngược nếu cần rollback.

Timeline Triển khai
Tuần 1-2

Module 3.1 — Farm Documents. Ít rủi ro nhất, thêm entity + API + UI upload.

Tuần 3-4

Module 3.3 — PHI Calculator. Xây reference data, validation logic. Unit test kỹ harvest safety check.

Tuần 5-7

Module 3.2 — Certification Engine. Phức tạp nhất, nhiều business rules. Cần đủ data từ 3.1 + 3.3.

Tuần 8-10

Module 3.4 — public Traceability. Mở rộng marketplace endpoints, thêm public routes. Security review cần thiết.

Tuần 11-12

Module 4.1 — UI/UX Optimization. Refactor frontend, SearchWindow component. Song song với backend.

Tuần 13-16

Module 4.2 — Delivery Integration. Microservice mới, phát triển độc lập. Demo mode → real provider APIs.

Database Migration Plan
V4__farm_documents.sql — Bảng farm_documents trong farm_db

V5__pesticide_records.sql — Bảng pesticide_records + pesticide_phi_reference trong season_db

V6__certification.sql — Bảng certification_standards + certification_records + checklist_items trong farm_db

V7__marketplace_extensions.sql — Columns certification_badge, pesticide_summary (nullable, backward compatible)

V8__delivery.sql — Tạo hoàn toàn delivery_db cho Delivery Service mới

Tài liệu Tham khảo
VietGAP TCVN 11892-1:2017 (trồng trọt) | QĐ 4653/QĐ-BNN-CN (chăn nuôi VietGAHP) | QĐ 3824/QĐ-BNN-TCTS (thủy sản) | EPA Pesticide PHI Database | GHTK/GHN API documentation | FAO traceability guidelines | Dữ liệu trong AI_CHATBOX/data/vietgap/ và AI_CHATBOX/data/traceability/ của project
Plan created by Cursor Agent based on VietFuture2026 codebase analysis and real-world Vietnamese agricultural certification standards research.