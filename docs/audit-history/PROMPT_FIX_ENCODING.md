# PROMPT (Bước 2) — Chuẩn hoá Encoding UTF-8 toàn repo VietFuture2026

> Dán tiếp prompt này cho AI coding agent, SAU KHI đã xác nhận Bước 1 (fix `\"` + Tooltip trùng) đã hoàn tất đúng.

---

## VAI TRÒ

Bạn tiếp tục là Senior Full-stack Engineer đang dọn nợ kỹ thuật cho repo `VietFuture2026`. Ở bước trước, bạn (hoặc một agent khác) đã phát hiện: **project có file lưu UTF-8, có file fallback về ANSI/Windows-125x (rất có thể là `Windows-1258` — bảng mã tiếng Việt phổ biến trên Windows, hoặc `Windows-1252`)**. Nhiệm vụ lần này: **xác định chính xác từng file, chuyển toàn bộ về UTF-8 thật một cách AN TOÀN — không được đoán mò hoặc làm mất thêm dữ liệu tiếng Việt.**

## TẠI SAO VIỆC NÀY QUAN TRỌNG (đọc để hiểu rủi ro, không chỉ làm theo lệnh)

Toàn bộ `pom.xml` của các service (`farm-service`, `season-service`, `marketplace-service`...) đã khai báo:
```xml
<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
```
Nghĩa là **Maven/javac sẽ đọc MỌI file `.java` như thể nó là UTF-8**, bất kể file đó thực sự được lưu bằng encoding gì. Nếu 1 file thực chất là Windows-1258/1252, hậu quả có thể là:
- **Build lỗi thẳng** với `unmappable character for encoding UTF-8` (nếu byte không tạo thành chuỗi UTF-8 hợp lệ), hoặc
- **Tệ hơn — build vẫn qua nhưng chữ tiếng Việt trong message/log/validation bị sai/lỗi font (mojibake)** khi chạy thật — loại lỗi này **không bị chặn ở bước biên dịch nên rất dễ lọt xuống production**.

Đã xác nhận thực tế 1 ví dụ điển hình: `season-service/src/main/java/org/example/season/dto/request/AiDiseaseSuggestionRequest.java` — chuỗi message tiếng Việt trong `@NotBlank(message = "...")` chứa byte cao (0xEA, 0xF4...) không hợp lệ UTF-8, XEN LẪN với ký tự `?` literal ở một số vị trí khác trong cùng chuỗi. **Điều này có nghĩa một phần dữ liệu tiếng Việt CÓ THỂ đã bị mất thật (không phải chỉ sai encoding) từ một lần convert lỗi trước đó** — không phải trường hợp nào cũng phục hồi được bằng cách đổi encoding.

→ **Nguyên tắc bắt buộc: KHÔNG được đoán/tự bịa lại dấu tiếng Việt nếu dữ liệu gốc đã mất. Chỉ được sửa những chỗ có thể phục hồi chính xác 100% bằng cách giải mã đúng byte gốc.**

---

## QUY TRÌNH BẮT BUỘC — làm đúng thứ tự, không rút gọn

### Bước 1 — Quét toàn repo, phân loại từng file theo encoding thật

Viết 1 script nhỏ (Python, dùng thư viện chuẩn — khuyến khích `charset-normalizer` hoặc `chardet` nếu có sẵn, không có thì dùng phương pháp thử-giải-mã bên dưới) quét **toàn bộ** file text trong repo (không chỉ 8 file đã sửa ở Bước 1): `*.java`, `*.sql`, `*.yml`, `*.yaml`, `*.properties`, `*.xml`, `*.ts`, `*.tsx`, `*.md`. Với mỗi file:

1. Đọc file ở dạng **bytes** (không mở bằng text mode mặc định của OS, tránh auto-decode sai ngay từ đầu).
2. Thử `bytes.decode('utf-8', errors='strict')`. Nếu **thành công** → file đã đúng UTF-8, bỏ qua, không đụng vào.
3. Nếu **thất bại** (raise `UnicodeDecodeError`) → đây là file nghi vấn, đưa vào danh sách xử lý tiếp ở Bước 2.

Output của bước này: 1 danh sách đầy đủ, chính xác các file KHÔNG PHẢI UTF-8 hợp lệ — đây là "nguồn sự thật" duy nhất để làm tiếp, không dùng phỏng đoán của lần audit trước (lần trước chỉ soi được 1 file ví dụ, có thể còn nhiều file khác chưa lộ ra).

### Bước 2 — Với mỗi file nghi vấn: thử giải mã đúng bảng mã gốc

Với mỗi file ở danh sách Bước 1, thử theo đúng thứ tự ưu tiên sau (dừng ngay khi có 1 phương án thành công VÀ hợp lý):

1. Thử `bytes.decode('windows-1258', errors='strict')` (bảng mã tiếng Việt phổ biến nhất trên Windows — ưu tiên thử trước vì đây là dự án tiếng Việt).
2. Nếu lỗi, thử `bytes.decode('windows-1252', errors='strict')`.
3. Nếu lỗi, thử `bytes.decode('cp1258', errors='strict')` / `cp1252` (một số thư viện phân biệt tên khác nhau).

**Sau khi decode "thành công" theo kỹ thuật (không raise lỗi), BẮT BUỘC kiểm tra thêm bằng mắt/heuristic trước khi tin:**
- Chuỗi kết quả có xuất hiện ký tự điều khiển lạ (control character ngoài `\n`, `\t`, `\r`) không? Nếu có → khả năng cao là sai bảng mã, thử phương án tiếp theo.
- Nếu file có chứa các từ tiếng Việt nhận diện được (biến, comment, message), kết quả sau decode có **đọc được thành câu tiếng Việt có nghĩa** không (có dấu đúng vị trí, không lộn xộn)? Nếu không chắc → liệt kê ra để người dùng tự xem, **không tự quyết**.
- Nếu trong chuỗi gốc đã có ký tự `?` literal xen giữa các ký tự khác của cùng 1 từ có dấu (dấu hiệu dữ liệu đã mất trước đó, như ví dụ `AiDiseaseSuggestionRequest.java`) → **đây là dữ liệu KHÔNG THỂ phục hồi tự động**. KHÔNG tự đoán chữ đúng. Đưa vào danh sách "CẦN NGƯỜI XEM LẠI" ở báo cáo cuối, giữ nguyên nội dung hiện tại (chỉ đổi encoding lưu file sang UTF-8, không sửa nội dung chữ).

### Bước 3 — Ghi lại file bằng UTF-8 thật (không BOM)

Với các file đã xác định chắc chắn encoding gốc và decode ra kết quả hợp lý ở Bước 2: ghi đè lại file bằng encode `utf-8` (không thêm BOM — `encode('utf-8')` chuẩn, không dùng `utf-8-sig`), giữ nguyên line ending hiện có của file (đa số đã là LF sau đợt fix trước — không tự đổi CRLF/LF ở bước này, đó không phải phạm vi công việc lần này).

### Bước 4 — Xác nhận lại bằng chính công cụ build sẽ dùng

```bash
# Bắt lỗi encoding thật, đúng cấu hình pom.xml khai báo:
find farm-service/src/main/java season-service/src/main/java marketplace-service/src/main/java \
     delivery-service/src/main/java crop-catalog-service/src/main/java -name "*.java" > /tmp/all.txt
javac -nowarn -Xlint:none -encoding UTF-8 -d /tmp/out @/tmp/all.txt 2>&1 \
  | grep -E "unmappable character|illegal character|error: "
```
Mục tiêu: **0 dòng lỗi liên quan `unmappable character`**. (Vẫn sẽ có nhiều `cannot find symbol` do thiếu Maven dependency trên classpath — bỏ qua như lần trước, chỉ quan tâm lỗi encoding/cú pháp.)

Ngoài ra, xác nhận nhanh bằng Python cho toàn bộ danh sách file đã sửa: đọc lại bằng `open(path, encoding='utf-8', errors='strict').read()` — phải chạy không lỗi cho tất cả.

---

## PHÒNG NGỪA VỀ SAU (làm 1 lần, giá trị lâu dài — đúng như đề xuất "nên cấu hình lưu toàn bộ sang UTF-8")

Tạo 2 file cấu hình ở root repo (nếu chưa có — đã xác nhận hiện tại **chưa có cả hai**):

**`.editorconfig`** (root):
```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.md]
trim_trailing_whitespace = false
```

**`.gitattributes`** (root):
```
* text=auto eol=lf
*.java text eol=lf
*.sql  text eol=lf
*.yml  text eol=lf
*.ts   text eol=lf
*.tsx  text eol=lf
```

Hai file này không sửa nội dung code, chỉ thiết lập quy ước cho IDE/Git từ nay về sau — đây chính là gốc rễ để tránh lặp lại cả 2 lỗi đã gặp (line-ending literal `\r\n` ở Bước 1 lẫn encoding lẫn lộn ở bước này).

---

## RÀNG BUỘC — KHÔNG ĐƯỢC LÀM

- **KHÔNG** tự đoán/bịa dấu tiếng Việt cho phần dữ liệu đã xác định là mất thật (có `?` literal xen giữa).
- **KHÔNG** sửa bất kỳ logic/tên biến/cấu trúc code nào — chỉ đổi encoding lưu file.
- **KHÔNG** chạy `mvn compile/package/install`, **KHÔNG** chạy `npm run build/typecheck` toàn bộ, **KHÔNG** chạy `docker compose up`. Chỉ dùng `javac -encoding UTF-8` cục bộ như Bước 4 để tự kiểm tra.
- **KHÔNG** động vào 8 file đã sửa ở Bước 1 nếu chúng đã pass kiểm tra UTF-8 (Bước 1 quy trình này) — chỉ xử lý file THỰC SỰ không phải UTF-8.

## BÁO CÁO KẾT QUẢ — đúng format sau

```
✅ Đã quét toàn repo: <N> file không phải UTF-8 hợp lệ được phát hiện
✅ Đã chuyển đổi thành công sang UTF-8: <liệt kê file + bảng mã gốc xác định được, vd "windows-1258">
⚠️ CẦN NGƯỜI XEM LẠI (nghi có mất dữ liệu tiếng Việt, KHÔNG tự đoán): <liệt kê file + vị trí dòng cụ thể>
✅ Đã tạo .editorconfig + .gitattributes ở root
✅ Kiểm tra javac -encoding UTF-8: 0 lỗi unmappable character
```

**Sau đó dừng lại, không tự build. Báo cho tôi tự chạy `mvn clean install` và `npm run build` để xác nhận cuối cùng — cả 2 bước (fix `\"`/Tooltip + chuẩn hoá encoding) đã xong về phần bạn.**
