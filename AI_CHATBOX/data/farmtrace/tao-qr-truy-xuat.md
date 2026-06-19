---
doc_id: farmtrace__tao_qr_truy_xuat
title: Tạo QR truy xuất nguồn gốc
category: farmtrace
audience: farmer
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [farmtrace, qr, truy-xuat]
source_type: internal_synthesis
review_required: true
---

# Tạo QR truy xuất nguồn gốc

## Mục đích

Hướng dẫn tạo QR để người mua xem thông tin công khai về lô hàng, nông trại, mùa vụ và nhật ký sản xuất đã được phép hiển thị.

## Điều kiện trước khi tạo QR

QR nên được tạo cho lô hàng đã có dữ liệu nền. Tối thiểu cần có nông trại, mùa vụ, phiếu thu hoạch, nhập kho và thông tin sản phẩm.

Không nên tạo QR chỉ chứa tên sản phẩm nhưng không liên kết được về lô hàng. QR như vậy không đủ giá trị truy xuất.

## Dữ liệu QR nên liên kết

- Mã QR hoặc mã truy xuất.
- Mã lô hàng.
- Sản phẩm.
- Nông trại.
- Mùa vụ.
- Ngày thu hoạch.
- Phiếu nhập kho.
- Nhật ký sản xuất công khai.
- Trạng thái chứng nhận nếu có.

## Các bước thực hiện

1. Chọn lô hàng cần truy xuất.
2. Kiểm tra dữ liệu mùa vụ và kho.
3. Chọn tạo QR.
4. Chọn thông tin được phép hiển thị công khai.
5. Lưu QR.
6. Kiểm tra trang truy xuất bằng cách quét thử.
7. Gắn QR vào sản phẩm hoặc trang bán hàng.

## Thông tin không nên hiển thị

- Dữ liệu nội bộ của admin.
- Ghi chú kiểm tra chưa công khai.
- Thông tin cá nhân nhạy cảm.
- File giấy tờ đầy đủ nếu không cần thiết.
- Token, ID nội bộ, đường dẫn file riêng tư.

## Lỗi thường gặp

- QR không liên kết lô hàng.
- QR hiển thị quá nhiều thông tin nhạy cảm.
- QR vẫn hoạt động dù lô hàng bị hủy hoặc hết hạn.
- Người mua quét QR nhưng không thấy ngày thu hoạch hoặc nông trại.
