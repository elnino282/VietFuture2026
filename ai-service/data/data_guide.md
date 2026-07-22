# ACM RAG Data Guide

Tài liệu này quy định cách viết, đặt tên và chuẩn hóa nội dung trong thư mục `data/` để nạp vào hệ thống RAG chatbot ACM.

Mục tiêu là giúp chatbot trả lời rõ ràng, đúng phạm vi tài liệu, hạn chế bịa thông tin và dễ truy xuất nguồn.

## 1. Nguyên tắc chung

Tất cả tài liệu dùng cho RAG cần tuân thủ các nguyên tắc sau:

* Viết bằng tiếng Việt rõ ràng, dễ hiểu.
* Ưu tiên câu ngắn, trực tiếp.
* Mỗi file chỉ nên tập trung vào một chủ đề chính.
* Nội dung phải chia heading rõ ràng bằng Markdown.
* Không đưa quá nhiều chủ đề không liên quan vào cùng một file.
* Không đưa dữ liệu cá nhân, mật khẩu, token, khóa API hoặc thông tin nhạy cảm.
* Không ghi thông tin chưa chắc chắn như một sự thật tuyệt đối.
* Nếu dữ liệu chưa đủ, nội dung nên ghi rõ giới hạn hoặc phạm vi áp dụng.
* Với tài liệu VietGAP tổng hợp, cần tránh bịa số liệu, tiêu chuẩn hoặc quy định pháp lý nếu chưa có nguồn xác nhận.

## 2. Quy tắc đặt tên file

Tên file phải:

* Viết chữ thường.
* Không dấu tiếng Việt.
* Dùng dấu gạch ngang `-` để phân tách từ.
* Có đuôi `.md`.
* Ngắn gọn nhưng đủ nghĩa.

Ví dụ tốt:

```txt
tong-quan-vietgap.md
yeu-cau-dat-nuoc-giong.md
tao-mua-vu.md
ghi-nhat-ky-san-xuat.md
bao-mat-qr.md
faq-nong-dan.md
```

Ví dụ không nên dùng:

```txt
Tài liệu VietGAP mới nhất.docx
huongdan.txt
file1.md
data copy.md
QR_final_update_last.md
```

## 3. Cấu trúc chuẩn của một file Markdown

Mỗi file nên có cấu trúc cơ bản:

```md
# Tiêu đề tài liệu

## Mục đích

## Đối tượng sử dụng

## Nội dung chính

## Các bước thực hiện

## Lưu ý

## Câu hỏi thường gặp
```

Không bắt buộc file nào cũng có đầy đủ các mục trên. Tuy nhiên, file cần có ít nhất:

```md
# Tiêu đề tài liệu

## Mục đích

## Nội dung chính
```

## 4. Front matter khuyến nghị

Mỗi file `.md` nên có phần metadata ở đầu file:

```md
---
doc_id: ACM__tao-mua-vu
title: Tạo mùa vụ trong ACM
category: acm
audience: farmer
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [mua-vu, nong-dan, acm]
---
```

## 5. Ý nghĩa các trường metadata

| Trường       | Bắt buộc | Ý nghĩa                      |
| ------------ | -------- | ---------------------------- |
| `doc_id`     | Có       | ID ổn định của tài liệu      |
| `title`      | Có       | Tiêu đề tài liệu             |
| `category`   | Có       | Nhóm tài liệu                |
| `audience`   | Có       | Đối tượng sử dụng            |
| `language`   | Có       | Ngôn ngữ tài liệu            |
| `version`    | Nên có   | Phiên bản tài liệu           |
| `updated_at` | Nên có   | Ngày cập nhật                |
| `tags`       | Nên có   | Từ khóa phục vụ tìm kiếm/lọc |

## 6. Giá trị chuẩn cho `category`

Chỉ nên dùng các giá trị sau:

| Giá trị        | Ý nghĩa                      |
| -------------- | ---------------------------- |
| `vietgap`      | Kiến thức VietGAP            |
| `crop`         | Kiến thức theo cây trồng     |
| `acm`          | Hướng dẫn hệ thống ACM       |
| `traceability` | Truy xuất nguồn gốc          |
| `template`     | Mẫu biểu, nhật ký            |
| `faq`          | Câu hỏi thường gặp           |

## 7. Giá trị chuẩn cho `audience`

Chỉ nên dùng các giá trị sau:

| Giá trị  | Ý nghĩa                              |
| -------- | ------------------------------------ |
| `farmer` | Nông dân, chủ nông trại              |
| `buyer`  | Người mua, người quét QR             |
| `admin`  | Quản trị viên hệ thống               |
| `all`    | Dùng chung cho nhiều nhóm người dùng |

## 8. Cách viết tài liệu VietGAP

Tài liệu VietGAP nên viết theo hướng dễ hiểu, thực tế, hỗ trợ nông dân chuẩn bị quy trình và hồ sơ.

Cấu trúc khuyến nghị:

```md
# Tên chủ đề VietGAP

## Mục đích

## Yêu cầu chính

## Việc nông dân cần làm

## Hồ sơ hoặc nhật ký cần chuẩn bị

## Lỗi thường gặp

## Lưu ý
```

Ví dụ:

```md
# Yêu cầu về phân bón và thuốc bảo vệ thực vật

## Mục đích
Giúp nông dân sử dụng phân bón và thuốc bảo vệ thực vật an toàn, có ghi nhận nhật ký đầy đủ.

## Việc nông dân cần làm
- Ghi lại loại phân bón hoặc thuốc đã sử dụng.
- Ghi ngày sử dụng, liều lượng và khu vực áp dụng.
- Không sử dụng sản phẩm không rõ nguồn gốc.
- Tuân thủ thời gian cách ly trước thu hoạch.
```

## 9. Cách viết tài liệu ACM

Tài liệu ACM nên mô tả đúng chức năng thật của hệ thống.

Cấu trúc khuyến nghị:

```md
# Tên chức năng

## Mục đích

## Đối tượng sử dụng

## Khi nào sử dụng

## Các bước thực hiện

## Dữ liệu cần nhập

## Kết quả sau khi hoàn thành

## Lỗi thường gặp

## Câu hỏi thường gặp
```

Ví dụ:

```md
# Tạo mùa vụ trong ACM

## Mục đích
Giúp nông dân tạo một mùa vụ mới để theo dõi quá trình sản xuất.

## Đối tượng sử dụng
Nông dân hoặc chủ nông trại.

## Các bước thực hiện
1. Vào mục Quản lý mùa vụ.
2. Chọn Tạo mùa vụ.
3. Nhập thông tin cây trồng, diện tích và ngày bắt đầu.
4. Chọn nông trại liên kết.
5. Lưu mùa vụ.

## Kết quả sau khi hoàn thành
Hệ thống tạo một mùa vụ mới để nông dân ghi nhật ký sản xuất và phục vụ truy xuất nguồn gốc.
```

## 10. Cách viết tài liệu truy xuất nguồn gốc

Tài liệu truy xuất nguồn gốc cần làm rõ dữ liệu QR lấy từ đâu, hiển thị gì và bảo mật thế nào.

Cấu trúc khuyến nghị:

```md
# Tên chủ đề truy xuất nguồn gốc

## Mục đích

## Dữ liệu đầu vào

## Quy trình xử lý

## Thông tin hiển thị cho người mua

## Thông tin không nên hiển thị công khai

## Lưu ý bảo mật

## Câu hỏi thường gặp
```

Ví dụ:

```md
# Thông tin hiển thị khi quét QR

## Thông tin người mua có thể xem
- Tên sản phẩm.
- Tên nông trại.
- Khu vực sản xuất.
- Ngày thu hoạch.
- Thông tin lô hàng.
- Nhật ký sản xuất đã được công khai.
- Trạng thái chứng nhận hoặc kiểm tra nếu có.

## Thông tin không nên hiển thị
- Dữ liệu nội bộ của admin.
- Thông tin cá nhân nhạy cảm.
- Dữ liệu chưa được xác minh.
```

## 11. Cách viết template nhật ký

Template nên viết theo dạng bảng để chatbot dễ hướng dẫn người dùng điền thông tin.

Ví dụ:

```md
# Mẫu nhật ký phân bón

## Mục đích
Ghi nhận quá trình sử dụng phân bón trong mùa vụ.

## Mẫu thông tin cần ghi

| Trường thông tin | Mô tả | Bắt buộc |
|---|---|---|
| Ngày bón phân | Ngày thực hiện bón phân | Có |
| Loại phân bón | Tên phân bón sử dụng | Có |
| Liều lượng | Khối lượng hoặc nồng độ sử dụng | Có |
| Khu vực áp dụng | Luống, khu vực hoặc diện tích áp dụng | Có |
| Người thực hiện | Người ghi nhận hoặc thực hiện | Nên có |
| Ghi chú | Thông tin bổ sung | Không |
```

## 12. Cách viết FAQ

FAQ nên viết ngắn, trực tiếp, theo dạng câu hỏi - trả lời.

Cấu trúc khuyến nghị:

```md
# FAQ chủ đề

## Câu hỏi: Nội dung câu hỏi?
Trả lời: Nội dung trả lời ngắn gọn, rõ ràng.

## Câu hỏi: Nội dung câu hỏi khác?
Trả lời: Nội dung trả lời.
```

Ví dụ:

```md
# FAQ ACM

## Câu hỏi: Nông dân cần tạo nông trại trước hay tạo mùa vụ trước?
Trả lời: Nông dân nên tạo nông trại trước, sau đó tạo mùa vụ và liên kết mùa vụ với nông trại đó.

## Câu hỏi: Sau thu hoạch có cần nhập kho không?
Trả lời: Có. Sau khi thu hoạch, nông dân nên nhập kho để hệ thống ghi nhận sản lượng, lô hàng và phục vụ đăng bán sản phẩm.
```

## 13. Quy tắc chunk-friendly

Để chatbot RAG truy xuất tốt, nội dung nên:

* Mỗi heading chỉ chứa một nhóm ý chính.
* Mỗi đoạn nên từ 1 đến 4 câu.
* Danh sách nên dùng bullet hoặc bảng.
* Quy trình nên đánh số bước.
* Không viết đoạn quá dài trên 200-300 từ.
* Không trộn câu hỏi và nhiều câu trả lời khác nhau trong cùng một đoạn.
* Không để thông tin quan trọng chỉ nằm trong tên file.

## 14. Những nội dung không nên đưa vào data

Không đưa các nội dung sau vào tài liệu RAG:

* Mật khẩu, token, API key.
* Thông tin cá nhân nhạy cảm.
* Dữ liệu người dùng thật.
* Đường dẫn nội bộ không cần thiết.
* Log lỗi dài không liên quan.
* Nội dung chưa kiểm chứng nhưng viết như tiêu chuẩn chính thức.
* Nội dung copy trùng lặp giữa nhiều file.
* Nội dung quá chung chung, không giúp chatbot trả lời câu hỏi thực tế.

## 15. Checklist trước khi nạp vào RAG

Trước khi chạy ingest, kiểm tra:

```txt
[ ] File dùng định dạng .md
[ ] Tên file không dấu, chữ thường, dùng dấu gạch ngang
[ ] File có heading # rõ ràng
[ ] Nội dung chia thành các mục ## hoặc ###
[ ] Có metadata đầu file nếu cần
[ ] Có dòng tương ứng trong sources.jsonl
[ ] Nội dung không trùng lặp quá nhiều với file khác
[ ] Không chứa thông tin nhạy cảm
[ ] Không chứa dữ liệu người dùng thật
[ ] Nội dung đúng với chức năng thật của ACM
[ ] Nếu là VietGAP, không bịa tiêu chuẩn hoặc số liệu chưa xác minh
```

## 16. Quy tắc cập nhật tài liệu

Khi sửa tài liệu:

* Nếu sửa lỗi chính tả hoặc câu chữ nhỏ: tăng patch version, ví dụ `1.0` thành `1.0.1`.
* Nếu thêm nội dung mới nhưng không thay đổi cấu trúc chính: tăng minor version, ví dụ `1.0` thành `1.1`.
* Nếu thay đổi lớn nội dung hoặc phạm vi tài liệu: tăng major version, ví dụ `1.0` thành `2.0`.

Sau khi cập nhật, cần sửa lại dòng tương ứng trong `sources.jsonl`.

## 17. Câu trả lời khi tài liệu không đủ

Nếu tài liệu chưa có đủ thông tin, chatbot nên trả lời theo hướng:

```txt
Tôi chưa có đủ dữ liệu trong tài liệu hiện tại để trả lời chính xác nội dung này.
```

Không nên tự bịa tiêu chuẩn, số liệu, quy định pháp lý hoặc chức năng chưa có trong hệ thống.
