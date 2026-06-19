---
doc_id: traceability__kiem_tra_tinh_hop_le_qr
title: Kiểm tra tính hợp lệ của QR
category: traceability
audience: admin
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [traceability, qr, kiem-tra]
source_type: internal_synthesis
review_required: true
---

# Kiểm tra tính hợp lệ của QR

## Mục đích

Giúp admin và hệ thống kiểm tra QR trước khi cho phép hiển thị hoặc in lên sản phẩm.

## QR hợp lệ cần có

- Có mã QR hoặc mã truy xuất ổn định.
- Có liên kết với lô hàng.
- Lô hàng còn tồn tại và không bị hủy.
- Lô hàng có phiếu nhập kho.
- Phiếu nhập kho liên kết với thu hoạch.
- Thu hoạch liên kết với mùa vụ.
- Mùa vụ liên kết với nông trại.
- Thông tin công khai đã được lọc dữ liệu nhạy cảm.

## Trạng thái QR đề xuất

| Trạng thái | Ý nghĩa |
|---|---|
| Draft | QR mới tạo, chưa phát hành |
| Active | QR đang hoạt động |
| Suspended | QR tạm dừng do cần kiểm tra |
| Revoked | QR bị thu hồi hoặc không còn hợp lệ |
| Expired | QR hết hạn theo chính sách nội bộ nếu có |

## Quy trình kiểm tra

1. Kiểm tra mã QR có tồn tại trong hệ thống.
2. Kiểm tra trạng thái QR.
3. Kiểm tra lô hàng liên kết.
4. Kiểm tra chuỗi nông trại → mùa vụ → thu hoạch → kho.
5. Kiểm tra chứng nhận nếu QR hiển thị chứng nhận.
6. Quét thử QR bằng thiết bị người dùng.
7. Xác nhận hoặc tạm dừng nếu phát hiện vấn đề.

## Lưu ý bảo mật

Không để QR chứa token bí mật hoặc dữ liệu nội bộ. QR nên dẫn đến một URL truy xuất công khai được kiểm soát quyền hiển thị.
