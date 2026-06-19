---
doc_id: farmtrace__quan_ly_chung_nhan_vietgap
title: Quản lý chứng nhận VietGAP
category: farmtrace
audience: admin
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [farmtrace, vietgap, chung-nhan]
source_type: internal_synthesis
review_required: true
---

# Quản lý chứng nhận VietGAP

## Mục đích

Hướng dẫn quản lý trạng thái chứng nhận VietGAP trong FarmTrace để tránh hiển thị sai thông tin cho người mua.

## Ai sử dụng

Nông dân có thể tải lên giấy chứng nhận hoặc thông tin liên quan. Admin kiểm tra và xác nhận trạng thái trước khi hệ thống hiển thị công khai.

## Dữ liệu cần quản lý

| Trường thông tin | Mô tả |
|---|---|
| Tên chứng nhận | Tên chứng nhận hoặc tiêu chuẩn |
| Số chứng nhận | Mã giấy chứng nhận nếu có |
| Đơn vị cấp | Tổ chức chứng nhận |
| Ngày cấp | Ngày bắt đầu hiệu lực |
| Ngày hết hạn | Ngày hết hiệu lực |
| Phạm vi | Nông trại, mùa vụ, cây trồng hoặc sản phẩm áp dụng |
| File đính kèm | Ảnh hoặc PDF giấy chứng nhận |
| Trạng thái | Chờ duyệt, hợp lệ, hết hạn, bị từ chối |

## Quy trình xử lý

1. Nông dân tải lên chứng nhận.
2. Hệ thống đặt trạng thái chờ duyệt.
3. Admin kiểm tra số chứng nhận, ngày hiệu lực và phạm vi.
4. Admin xác nhận hoặc yêu cầu bổ sung.
5. Hệ thống chỉ hiển thị chứng nhận công khai khi trạng thái hợp lệ.
6. Khi hết hạn, hệ thống nên cảnh báo hoặc tự chuyển trạng thái nếu có cấu hình.

## Lưu ý hiển thị QR

Chỉ nên hiển thị thông tin chứng nhận có liên quan đến lô hàng. Nếu chứng nhận áp dụng cho nông trại nhưng không áp dụng cho sản phẩm hoặc mùa vụ cụ thể, chatbot nên trả lời thận trọng.

## Lỗi thường gặp

- Upload chứng nhận hết hạn nhưng vẫn hiển thị hợp lệ.
- Không ghi phạm vi chứng nhận.
- Dùng chứng nhận của nông trại khác cho lô hàng hiện tại.
- Hiển thị file giấy tờ có dữ liệu nhạy cảm cho người mua.
