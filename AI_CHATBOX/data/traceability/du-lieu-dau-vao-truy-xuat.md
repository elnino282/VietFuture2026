---
doc_id: traceability__du_lieu_dau_vao_truy_xuat
title: Dữ liệu đầu vào cho truy xuất nguồn gốc
category: traceability
audience: all
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [traceability, du-lieu-dau-vao, lo-hang]
source_type: internal_synthesis
review_required: true
---

# Dữ liệu đầu vào cho truy xuất nguồn gốc

## Mục đích

Liệt kê dữ liệu cần có trước khi tạo QR để đảm bảo người mua có thể truy xuất được thông tin có ý nghĩa.

## Dữ liệu nông trại

- Tên nông trại.
- Khu vực sản xuất.
- Người phụ trách hoặc đơn vị quản lý.
- Chứng nhận hoặc hồ sơ liên quan nếu có.

## Dữ liệu mùa vụ

- Cây trồng.
- Diện tích hoặc khu vực sản xuất.
- Ngày bắt đầu.
- Giai đoạn hiện tại.
- Nhật ký sản xuất liên quan.

## Dữ liệu nhật ký

- Đất, nước, giống.
- Phân bón.
- Tưới nước.
- Sâu bệnh.
- Thuốc bảo vệ thực vật nếu có sử dụng.
- Thu hoạch.

## Dữ liệu kho và lô hàng

| Trường | Ý nghĩa |
|---|---|
| Mã lô hàng | Định danh lô để truy xuất |
| Ngày thu hoạch | Mốc quan trọng với người mua |
| Sản lượng | Số lượng hoặc khối lượng lô |
| Ngày nhập kho | Mốc quản lý sau thu hoạch |
| Trạng thái lô | Sẵn bán, tạm giữ, hủy hoặc hết hàng |
| Sản phẩm | Tên sản phẩm được bán |

## Dữ liệu chứng nhận

Chỉ đưa chứng nhận vào thông tin truy xuất nếu chứng nhận đã được xác minh, còn hiệu lực và đúng phạm vi áp dụng với nông trại, mùa vụ hoặc sản phẩm.

## Lưu ý

Không nên tạo QR khi dữ liệu đầu vào còn rỗng hoặc chỉ có tên sản phẩm. QR cần liên kết được với lô hàng và chuỗi dữ liệu nền.
