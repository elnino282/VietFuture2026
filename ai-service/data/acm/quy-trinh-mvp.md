---
doc_id: acm__quy_trinh_mvp
title: Quy trình MVP trên ACM
category: acm
audience: all
language: vi
version: 1.0
updated_at: 2026-06-24
tags: [acm, mvp, quy-trinh, luong, end-to-end, toan-bo-quy-trinh]
source_type: internal_demo
---

# Quy trình MVP trên ACM

## Mục đích

Mô tả luồng vận hành MVP (Minimum Viable Product) end-to-end của hệ thống ACM, từ tạo nông trại đến khi người mua quét QR.

## Luồng MVP tổng quan

1. **Tạo nông trại**: Nông dân đăng ký và tạo thông tin nông trại.
2. **Tạo mùa vụ**: Nông dân tạo mùa vụ mới, liên kết với nông trại.
3. **Ghi nhật ký sản xuất**: Ghi đất, nước, giống, phân bón, thuốc BVTV, chăm sóc, sâu bệnh.
4. **Admin kiểm tra**: Admin kiểm tra dữ liệu theo giai đoạn, yêu cầu bổ sung nếu thiếu.
5. **Thu hoạch**: Nông dân ghi nhận sản lượng thu hoạch.
6. **Nhập kho**: Sản phẩm được nhập kho, tạo lô hàng.
7. **Đăng bán**: Nông dân đăng bán sản phẩm, gắn lô hàng.
8. **Tạo QR**: Hệ thống tạo mã QR truy xuất nguồn gốc.
9. **Người mua quét QR**: Người mua xem thông tin sản phẩm, nông trại, lô hàng, mùa vụ.

## Vai trò trong MVP

### Nông dân
- Tạo nông trại, mùa vụ.
- Ghi nhật ký sản xuất.
- Thu hoạch, nhập kho.
- Đăng bán sản phẩm.

### Admin
- Kiểm tra hồ sơ nông trại.
- Duyệt dữ liệu mùa vụ theo giai đoạn.
- Quản lý QR và trạng thái công khai.

### Người mua
- Xem sản phẩm.
- Quét QR truy xuất nguồn gốc.
- Đặt hàng.

## Dữ liệu cần có cho MVP hoạt động

- Ít nhất 1 nông trại có thông tin đầy đủ.
- Ít nhất 1 mùa vụ có nhật ký sản xuất.
- Ít nhất 1 phiếu thu hoạch và nhập kho.
- Ít nhất 1 sản phẩm đăng bán có lô hàng.
- Ít nhất 1 QR có dữ liệu truy xuất.

## Sau khi tạo mùa vụ tôi cần làm gì tiếp theo?

Sau khi tạo mùa vụ, nông dân nên:

- Ghi thông tin đất, nước và giống ban đầu.
- Ghi nhật ký chăm sóc, bón phân.
- Ghi nhật ký thuốc BVTV nếu có sử dụng.
- Cập nhật sâu bệnh nếu phát hiện.
- Ghi thu hoạch khi có sản lượng.
- Nhập kho và đăng bán khi hoàn tất thu hoạch.

## Lưu ý

Tài liệu này là tài liệu tổng hợp nội bộ dùng cho demo đồ án ACM.
