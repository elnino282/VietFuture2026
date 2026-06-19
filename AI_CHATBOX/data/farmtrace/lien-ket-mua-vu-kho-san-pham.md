---
doc_id: farmtrace__lien_ket_mua_vu_kho_san_pham
title: Liên kết mùa vụ, kho và sản phẩm
category: farmtrace
audience: all
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [farmtrace, mua-vu, kho, san-pham]
source_type: internal_synthesis
review_required: true
---

# Liên kết mùa vụ, kho và sản phẩm

## Mục đích

Giúp đội phát triển và chatbot hiểu mối liên kết dữ liệu quan trọng nhất trong FarmTrace: mùa vụ tạo ra thu hoạch, thu hoạch đi vào kho, kho tạo lô hàng, lô hàng dùng để đăng bán và tạo QR.

## Mô hình liên kết đề xuất

```txt
Nông trại
  ↓
Mùa vụ
  ↓
Nhật ký sản xuất
  ↓
Thu hoạch
  ↓
Nhập kho
  ↓
Lô hàng
  ↓
Sản phẩm đăng bán
  ↓
QR truy xuất
```

## Vì sao liên kết này quan trọng

Nếu sản phẩm đăng bán không liên kết với lô hàng, người mua không thể truy xuất nguồn gốc. Nếu lô hàng không liên kết với nhập kho, hệ thống không kiểm soát được tồn kho. Nếu nhập kho không liên kết với thu hoạch, không thể đối chiếu sản lượng.

## Kiểm tra dữ liệu trước khi đăng bán

- Sản phẩm có lô hàng hay không?
- Lô hàng có phiếu nhập kho không?
- Phiếu nhập kho có phiếu thu hoạch không?
- Thu hoạch có mùa vụ không?
- Mùa vụ có nông trại không?
- Mùa vụ có nhật ký sản xuất chính không?

## Quy tắc xử lý lỗi

Nếu thiếu liên kết ở giữa chuỗi, hệ thống nên chặn bước tiếp theo hoặc cảnh báo rõ ràng. Ví dụ, không cho tạo QR khi chưa có lô hàng, không cho đăng bán vượt tồn kho, không cho hiển thị VietGAP khi chứng nhận chưa xác minh.

## Câu hỏi thường gặp

### Có thể đăng bán sản phẩm chưa nhập kho không?

Không nên. Với FarmTrace MVP, nên yêu cầu nhập kho để kiểm soát sản lượng và tạo lô hàng truy xuất.

### Có thể một mùa vụ có nhiều lô hàng không?

Có. Một mùa vụ có thể thu hoạch nhiều lần và tạo nhiều lô hàng khác nhau.
