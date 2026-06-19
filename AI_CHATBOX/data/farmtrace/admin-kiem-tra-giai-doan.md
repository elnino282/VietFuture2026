---
doc_id: farmtrace__admin_kiem_tra_giai_doan
title: Admin kiểm tra giai đoạn sản xuất
category: farmtrace
audience: admin
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [farmtrace, admin, kiem-tra-giai-doan]
source_type: internal_synthesis
review_required: true
---

# Admin kiểm tra giai đoạn sản xuất

## Mục đích

Mô tả cách admin kiểm tra dữ liệu mùa vụ theo từng giai đoạn để phát hiện thiếu hồ sơ sớm và tăng độ tin cậy của truy xuất nguồn gốc.

## Khi nào admin kiểm tra

Admin nên kiểm tra sau các mốc quan trọng: tạo mùa vụ, hoàn tất đầu vụ, trong quá trình chăm sóc, trước thu hoạch, sau thu hoạch và trước khi tạo QR hoặc đăng bán.

## Nội dung cần kiểm tra

| Giai đoạn | Dữ liệu cần xem |
|---|---|
| Đầu vụ | Nông trại, mùa vụ, đất, nước, giống |
| Chăm sóc | Phân bón, tưới nước, sâu bệnh |
| Phòng trừ | Thuốc BVTV, thời gian cách ly, lý do sử dụng |
| Trước thu hoạch | Nhật ký đầy đủ, cảnh báo rủi ro |
| Sau thu hoạch | Phiếu thu hoạch, nhập kho, sản lượng |
| Trước QR | Lô hàng, thông tin công khai, chứng nhận |

## Trạng thái xử lý

- Chờ kiểm tra: dữ liệu mới được gửi hoặc mới phát sinh.
- Cần bổ sung: thiếu thông tin hoặc cần nông dân giải thích.
- Đã duyệt: dữ liệu phù hợp với yêu cầu nội bộ.
- Từ chối: dữ liệu sai, không đủ căn cứ hoặc có rủi ro nghiêm trọng.

## Cách ghi nhận kết quả

Admin cần ghi rõ kết quả kiểm tra, ngày kiểm tra, người kiểm tra và lý do nếu yêu cầu bổ sung hoặc từ chối. Không nên chỉ đổi trạng thái mà không có ghi chú.

## Lỗi thường gặp

- Duyệt giai đoạn nhưng không xem nhật ký liên quan.
- Không kiểm tra liên kết giữa thu hoạch và kho.
- Không ghi lý do khi yêu cầu nông dân bổ sung.
- Duyệt QR khi lô hàng chưa đủ dữ liệu nền.
