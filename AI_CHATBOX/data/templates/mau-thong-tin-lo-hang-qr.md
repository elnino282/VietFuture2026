---
doc_id: template__mau_thong_tin_lo_hang_qr
title: Mẫu thông tin lô hàng QR
category: template
audience: all
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [template, lo-hang, qr]
source_type: internal_template
review_required: true
---

# Mẫu thông tin lô hàng QR

## Mục đích

Chuẩn hóa biểu mẫu `mau-thong-tin-lo-hang-qr.md` để chatbot FarmTrace hướng dẫn người dùng điền dữ liệu nhất quán, dễ kiểm tra và dễ truy xuất.

## Cách sử dụng

Mẫu này dùng để nông dân hoặc admin ghi dữ liệu theo cấu trúc thống nhất. Khi đưa vào hệ thống, mỗi dòng nên trở thành một bản ghi có ngày thực hiện, người thực hiện và mùa vụ liên kết.


## Mẫu thông tin cần ghi

| Trường thông tin | Mô tả | Bắt buộc |
|---|---|---|
| Mã lô hàng | Định danh lô hàng | Có |
| Tên sản phẩm | Tên hiển thị cho người mua | Có |
| Nông trại | Nơi sản xuất | Có |
| Mùa vụ | Mùa vụ liên kết | Có |
| Ngày thu hoạch | Ngày thu hoạch của lô | Có |
| Sản lượng | Sản lượng lô hàng | Nên có |
| Trạng thái chứng nhận | VietGAP hoặc chứng nhận khác nếu đã xác minh | Nếu có |
| Thông tin công khai | Các mục được phép hiển thị trên QR | Có |
| Thông tin ẩn | Thông tin không công khai | Nên có |

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
