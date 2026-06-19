---
doc_id: template__mau_phieu_nhap_kho
title: Mẫu phiếu nhập kho
category: template
audience: farmer
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [template, phieu-nhap-kho]
source_type: internal_template
review_required: true
---

# Mẫu phiếu nhập kho

## Mục đích

Chuẩn hóa biểu mẫu `mau-phieu-nhap-kho.md` để chatbot FarmTrace hướng dẫn người dùng điền dữ liệu nhất quán, dễ kiểm tra và dễ truy xuất.

## Cách sử dụng

Mẫu này dùng để nông dân hoặc admin ghi dữ liệu theo cấu trúc thống nhất. Khi đưa vào hệ thống, mỗi dòng nên trở thành một bản ghi có ngày thực hiện, người thực hiện và mùa vụ liên kết.


## Mẫu thông tin cần ghi

| Trường thông tin | Mô tả | Bắt buộc |
|---|---|---|
| Ngày nhập kho | Ngày sản phẩm vào kho | Có |
| Phiếu thu hoạch | Bản ghi thu hoạch liên kết | Có |
| Sản phẩm | Tên nông sản nhập kho | Có |
| Sản lượng nhập | Khối lượng hoặc số lượng nhập | Có |
| Đơn vị tính | Kg, bó, thùng hoặc đơn vị khác | Có |
| Mã lô hàng | Mã lô được tạo hoặc liên kết | Có |
| Trạng thái | Sẵn bán, tạm giữ, hủy, hết hàng | Có |
| Người nhập kho | Người thực hiện | Nên có |
| Ghi chú | Phân loại, điều kiện bảo quản | Không |

## Lưu ý nhập liệu

- Không bỏ trống trường bắt buộc.
- Ghi đúng ngày thực hiện, không ghi ngày ước đoán nếu không chắc.
- Nếu có ảnh, hóa đơn hoặc chứng từ, nên đính kèm vào bản ghi.
- Không đưa thông tin cá nhân nhạy cảm vào phần ghi chú.

## Câu hỏi thường gặp

### Có thể sửa bản ghi sau khi lưu không?

Có thể sửa nếu hệ thống cho phép, nhưng nên lưu lịch sử chỉnh sửa hoặc ghi chú lý do để admin kiểm tra.

### Có cần điền tất cả trường không?

Nên điền đủ trường bắt buộc. Các trường không bắt buộc vẫn hữu ích nếu cần chứng minh quy trình hoặc truy xuất chi tiết hơn.
