---
doc_id: traceability__quy_trinh_truy_xuat
title: Quy trình truy xuất nguồn gốc
category: traceability
audience: all
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [traceability, quy-trinh, qr]
source_type: internal_synthesis
review_required: true
---

# Quy trình truy xuất nguồn gốc

## Mục đích

Mô tả quy trình truy xuất nguồn gốc trong FarmTrace từ dữ liệu sản xuất đến thông tin người mua nhìn thấy khi quét QR.

## Nguyên tắc truy xuất

Truy xuất nguồn gốc phải trả lời được sản phẩm đến từ đâu, sản xuất trong mùa vụ nào, thu hoạch khi nào, nhập kho ra sao và thuộc lô hàng nào. QR chỉ là cổng truy cập dữ liệu, không phải là toàn bộ dữ liệu truy xuất.

## Luồng dữ liệu

1. Tạo nông trại.
2. Tạo mùa vụ.
3. Ghi nhật ký sản xuất.
4. Admin kiểm tra dữ liệu nếu có quy trình duyệt.
5. Thu hoạch và ghi sản lượng.
6. Nhập kho và tạo lô hàng.
7. Tạo QR liên kết với lô hàng.
8. Người mua quét QR.
9. Hệ thống hiển thị thông tin công khai.

## Dữ liệu truy xuất tối thiểu

| Nhóm dữ liệu | Mục đích |
|---|---|
| Nông trại | Xác định nơi sản xuất |
| Mùa vụ | Xác định đợt sản xuất |
| Nhật ký | Chứng minh quá trình sản xuất |
| Thu hoạch | Xác định ngày và sản lượng thu hoạch |
| Kho | Kiểm soát sản lượng và lô hàng |
| QR | Cho phép người mua truy cập thông tin công khai |

## Lỗi làm mất khả năng truy xuất

- Sản phẩm không có lô hàng.
- Lô hàng không liên kết với phiếu nhập kho.
- Phiếu nhập kho không liên kết với thu hoạch.
- Thu hoạch không liên kết với mùa vụ.
- Mùa vụ không có nhật ký sản xuất.
- QR bị tạo thủ công nhưng không gắn với dữ liệu hệ thống.
