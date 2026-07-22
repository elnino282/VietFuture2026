---
doc_id: traceability__bao_mat_va_gioi_han_du_lieu_qr
title: Bảo mật và giới hạn dữ liệu QR
category: traceability
audience: admin
language: vi
version: 1.0
updated_at: 2026-06-24
tags: [qr, bao-mat, gioi-han, du-lieu, quyen-rieng-tu, cong-khai]
source_type: internal_demo
---

# Bảo mật và giới hạn dữ liệu QR

## Mục đích

Hướng dẫn admin và nông dân về dữ liệu nào nên và không nên công khai qua QR truy xuất.

## Dữ liệu nên công khai qua QR

- Tên sản phẩm và nông trại.
- Khu vực sản xuất (không cần địa chỉ chính xác).
- Ngày thu hoạch.
- Thông tin lô hàng.
- Nhật ký sản xuất đã được kiểm tra.
- Trạng thái chứng nhận nếu có.

## Dữ liệu không nên công khai

- Thông tin cá nhân: số điện thoại, CMND, địa chỉ nhà nông dân.
- Ghi chú nội bộ của admin.
- Dữ liệu chưa xác minh.
- Thông tin tài chính nội bộ.
- Dữ liệu người mua.

## QR chứa dữ liệu gì?

QR thường chứa một đường dẫn (URL) đến trang truy xuất trên hệ thống, không chứa toàn bộ dữ liệu trực tiếp. Dữ liệu được quản lý trên hệ thống và hiển thị khi người mua truy cập trang.

## Quy tắc cho admin

- Kiểm tra dữ liệu công khai trước khi kích hoạt QR.
- Quét thử QR bằng vai trò người mua.
- Tạm dừng QR nếu phát hiện dữ liệu sai.
- Không công khai toàn bộ nhật ký nếu không cần thiết.

## QR bị thu hồi

Khi QR bị thu hồi hoặc tạm dừng, trang truy xuất nên hiển thị trạng thái không hợp lệ hoặc thông báo QR đã tạm dừng. Không hiển thị dữ liệu gây hiểu nhầm.

## Lưu ý

Tài liệu này là tài liệu tổng hợp nội bộ dùng cho demo đồ án ACM.
