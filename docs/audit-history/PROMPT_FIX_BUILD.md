# PROMPT — Fix triệt để lỗi chặn build Backend + Frontend (VietFuture2026)

> Dán nguyên văn prompt này cho AI coding agent (Claude Code, Cursor...) đang mở trực tiếp trong repo `VietFuture2026`.

---

## VAI TRÒ

Bạn là Senior Full-stack Engineer được giao xử lý một lỗi hỏng file mang tính hệ thống, phát sinh từ một lần áp dụng patch/AI-generate trước đó trong repo `VietFuture2026`. Lỗi đã được audit kỹ và mô tả đầy đủ dưới đây — **nhiệm vụ của bạn là SỬA ĐÚNG NHỮNG GÌ đã được xác định, không tự ý mở rộng phạm vi, không thiết kế lại logic nghiệp vụ.**

## BỐI CẢNH — ĐỌC KỸ TRƯỚC KHI SỬA

Một pipeline áp dụng patch trước đó đã ghi đè nhiều file với lỗi encode: các ký tự xuống dòng thật (`\n`) và dấu ngoặc kép thật (`"`) bị ghi thành **chuỗi văn bản literal** `\r\n` và `\"` (tức 2-4 ký tự thường, KHÔNG PHẢI ký tự điều khiển thật). Một lần fix trước đó đã sửa đúng phần `\r\n` → xuống dòng thật, nhưng **bỏ sót phần `\"` → dấu ngoặc kép thật**, khiến 8 file Java vẫn lỗi cú pháp, và 1 file frontend có lỗi export trùng lặp từ trước không liên quan.

Ví dụ cụ thể (byte thật lấy từ file đang lỗi):
```
// HIỆN TẠI (SAI — "\"" là 2 ký tự văn bản: backslash + dấu ngoặc kép):
Map.entry(\"APPLIED\", Set.of(\"AUDIT_SCHEDULED\"))

// PHẢI LÀ (ĐÚNG — dấu ngoặc kép thật):
Map.entry("APPLIED", Set.of("AUDIT_SCHEDULED"))
```

---

## PHẠM VI CÔNG VIỆC — 2 NHÓM, LÀM ĐÚNG THỨ TỰ

### NHÓM A (P0 — bắt buộc, chặn build Backend)

Sửa lỗi `\"` literal → `"` thật trong đúng **8 file** sau (đã xác nhận bằng `javac` thật, không phải suy đoán):

1. `farm-service/src/main/java/org/example/farm/service/CertificationAuditService.java`
2. `marketplace-service/src/main/java/org/example/marketplace/controller/MarketplaceAdminController.java`
3. `marketplace-service/src/main/java/org/example/marketplace/service/MarketplaceServiceImpl.java`
4. `season-service/src/main/java/org/example/season/service/ProductionDiaryAggregationService.java`
5. `season-service/src/main/java/org/example/season/service/EmployeeTrainingService.java`
6. `season-service/src/main/java/org/example/season/entity/EmployeeTrainingRecord.java`
7. `season-service/src/main/java/org/example/season/entity/TrainingProgram.java`
8. `season-service/src/main/java/org/example/season/client/SustainabilityServiceClient.java`

*(Đúng 8 file. Đường dẫn lấy từ lần audit gần nhất — vẫn ưu tiên tự `grep` xác nhận lại thay vì tin tuyệt đối, xem bước 1 quy trình, vì file có thể đã bị di chuyển/đổi tên từ đó tới nay.)*

**Cách xử lý từng file (BẮT BUỘC theo đúng quy trình, không tự rút gọn):**

1. **Xác định lại danh sách file thật sự còn lỗi** — đừng tin tuyệt đối danh sách trên, hãy tự quét trước:
   ```bash
   grep -rlP '\\"' --include="*.java" farm-service/src season-service/src marketplace-service/src delivery-service/src crop-catalog-service/src
   ```
   Đối chiếu kết quả với danh sách 8 file ở trên. Nếu có file phát sinh thêm ngoài danh sách, xử lý luôn theo cùng quy trình.

2. **Với mỗi file trong danh sách:** đọc toàn bộ nội dung, thay **mọi** chuỗi 2-ký-tự `\"` (backslash + dấu ngoặc kép) bằng 1 ký tự `"` (dấu ngoặc kép thật). Đây là phép thay thế văn bản toàn cục (global literal replace), **không phải** regex xử lý escape-sequence của ngôn ngữ nào — coi `\"` như một chuỗi 2 ký tự cần tìm-thay, không diễn giải nó.

3. **Sau khi thay, kiểm tra lại chính file đó** để chắc chắn:
   - Không còn ký tự `\` nào đứng ngoài string/char literal hoặc ngoài các escape hợp lệ (`\n`, `\t`, `\\`, `\'` bên trong string).
   - Số lượng dấu `"` trong file là **số chẵn** (mỗi string phải có mở-đóng đầy đủ).
   - Cấu trúc code (dấu `{`, `}`, `(`, `)`) cân bằng, không bị lệch do bước sửa.
   - Riêng `EmployeeTrainingRecord.java` và `TrainingProgram.java` là `@Entity` — kiểm tra kỹ các annotation JPA (`@Column(name = "...")`, `@Table(name = "...")`, `@JoinColumn(...)`) có đúng cú pháp sau khi sửa, vì đây là nơi Hibernate sẽ đọc trực tiếp.

4. **Xác nhận cục bộ bằng `javac` (không cần Maven, không cần mạng):**
   ```bash
   find farm-service/src/main/java season-service/src/main/java marketplace-service/src/main/java -name "*.java" > /tmp/all.txt
   javac -nowarn -Xlint:none -d /tmp/out @/tmp/all.txt 2>&1 | grep -E "illegal character|unclosed string literal|illegal start of|class, interface, enum"
   ```
   Lệnh này sẽ báo lỗi "cannot find symbol"/"package does not exist" hàng loạt — **bỏ qua các lỗi đó**, vì không có Maven dependency trên classpath là chuyện bình thường. Chỉ quan tâm output có còn dòng nào chứa `illegal character`, `unclosed string literal`, `illegal start of`, `class, interface, enum, or record expected` hay không — **mục tiêu là 0 dòng**.

### NHÓM B (P0 — bắt buộc, chặn build Frontend)

**File:** `agricultural-crop-management-frontend/src/shared/ui/index.ts`

File này export khối `Tooltip / TooltipContent / TooltipProvider / TooltipTrigger` **2 lần** (1 lần ở gần đầu file, 1 lần lặp lại gần dòng 400+). Đây là lỗi tồn tại từ trước, không liên quan gì tới các file ở Nhóm A.

**Việc cần làm:**
1. `grep -n "TooltipTrigger" agricultural-crop-management-frontend/src/shared/ui/index.ts` để xác định chính xác 2 vị trí khối export bị trùng (đừng tin số dòng cố định vì file có thể đã đổi từ lúc audit).
2. Xoá **1 trong 2** khối export trùng (giữ nguyên khối còn lại y hệt cũ, không đổi nội dung export).
3. Xác nhận: `grep -c "^  Tooltip,$" agricultural-crop-management-frontend/src/shared/ui/index.ts` (hoặc lệnh tương đương) phải trả về `1`, không phải `2`.

---

## SAU KHI SỬA XONG NHÓM A + B

- Chạy các lệnh kiểm tra tĩnh/local ở trên (bước javac + grep) để **tự xác nhận bằng chính công cụ sẵn có, không cần build đầy đủ**.
- **KHÔNG** chạy `mvn compile`/`mvn package`/`mvn install`.
- **KHÔNG** chạy `npm run build`/`npm run typecheck` đầy đủ dự án.
- **KHÔNG** chạy `docker compose up`.
- **KHÔNG** tự ý sửa thêm bất kỳ file nào ngoài phạm vi Nhóm A + B ở trên (kể cả khi thấy lỗi khác — chỉ ghi chú lại, không sửa).

Sau khi hoàn tất, báo cáo ngắn gọn cho tôi theo đúng format:

```
✅ Đã sửa xong Nhóm A: <liệt kê file đã sửa, số chỗ đã thay \" → " mỗi file>
✅ Đã sửa xong Nhóm B: đã xoá export trùng tại dòng <X-Y>
✅ Kiểm tra javac cục bộ: 0 lỗi illegal character / unclosed string literal
⚠️ Phát hiện thêm (nếu có, KHÔNG tự sửa): <mô tả>
```

**Sau đó dừng lại và yêu cầu tôi tự chạy build để xác nhận cuối cùng** (`mvn clean install` cho backend, `npm run build` cho frontend) — không tự chạy các lệnh build đầy đủ đó thay tôi.

---

## PHỤ LỤC — Danh sách 29 lỗi TypeScript và ~104 lỗi ESLint hiện có (KHÔNG thuộc phạm vi lần fix này)

Đây là nợ kỹ thuật tồn tại từ trước, độc lập với Nhóm A/B, **không xử lý trong lượt này**. Ghi nhận riêng để làm việc sau (đặc biệt lưu ý: có 2 khái niệm "đào tạo nhân viên" đang tồn tại song song — FE cũ dùng field `isTrained`/`trainingNotes` trên `SeasonEmployee`, BE mới của module VietGAP dùng bảng riêng `employee_training_records` — cần làm rõ có trùng nghiệp vụ hay không trước khi đụng vào, **tuyệt đối không tự gộp/xoá cái nào** trong lượt fix này).
