---
doc_id: traceability__lien_ket_qr_voi_lo_hang
title: Liên kết QR với lô hàng
category: traceability
audience: all
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [traceability, qr, lo-hang]
source_type: internal_synthesis
review_required: true
---

# Liên kết QR với lô hàng

## Mục đích

Giải thích vì sao QR phải liên kết với lô hàng thay vì chỉ liên kết với trang sản phẩm chung.

## Nguyên tắc liên kết

Một QR truy xuất nên trỏ đến một mã lô hàng hoặc mã truy xuất ổn định. Lô hàng liên kết với nhập kho, thu hoạch, mùa vụ và nông trại. Nhờ đó người mua có thể truy ngược toàn bộ chuỗi dữ liệu.

Nếu QR chỉ trỏ đến trang giới thiệu sản phẩm, người mua không biết sản phẩm cụ thể mình mua thuộc đợt thu hoạch nào.

## Dữ liệu liên kết cần có

| QR liên kết với | Để truy ra |
|---|---|
| Mã lô hàng | Sản lượng, trạng thái, ngày nhập kho |
| Phiếu thu hoạch | Ngày thu hoạch, khu vực, sản lượng |
| Mùa vụ | Cây trồng, nhật ký sản xuất |
| Nông trại | Nơi sản xuất, chứng nhận nếu có |
| Sản phẩm đăng bán | Giá bán, đơn vị tính, ảnh sản phẩm |

## Các bước kiểm tra

1. Mở chi tiết QR.
2. Kiểm tra QR có mã lô hàng hay không.
3. Kiểm tra lô hàng có phiếu nhập kho hay không.
4. Kiểm tra phiếu nhập kho có liên kết thu hoạch hay không.
5. Kiểm tra thu hoạch có mùa vụ hay không.
6. Kiểm tra thông tin công khai trước khi phát hành QR.

## Lỗi thường gặp

- Một QR dùng chung cho nhiều lô hàng không rõ điều kiện.
- QR không cập nhật khi lô hàng bị hủy.
- QR hiển thị sản phẩm nhưng không hiển thị ngày thu hoạch.
- QR bị sao chép sang sản phẩm không cùng lô.
