---
doc_id: traceability__thong_tin_hien_thi_qr
title: Thông tin hiển thị khi quét QR
category: traceability
audience: buyer
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [traceability, qr, buyer]
source_type: internal_synthesis
review_required: true
---

# Thông tin hiển thị khi quét QR

## Mục đích

Xác định thông tin người mua nên thấy khi quét QR truy xuất nguồn gốc.

## Thông tin nên hiển thị

- Tên sản phẩm.
- Tên nông trại.
- Khu vực sản xuất ở mức phù hợp.
- Tên cây trồng hoặc loại nông sản.
- Ngày thu hoạch.
- Mã lô hàng hoặc mã truy xuất.
- Trạng thái lô hàng nếu cần.
- Nhật ký sản xuất đã được công khai.
- Trạng thái chứng nhận nếu có và đã xác minh.

## Thông tin có thể hiển thị có điều kiện

- Ảnh nông trại.
- Ảnh mùa vụ hoặc ảnh thu hoạch.
- Tóm tắt nhật ký phân bón hoặc thuốc BVTV.
- Kết quả kiểm tra nội bộ.
- Thông tin đóng gói hoặc bảo quản.

## Thông tin không nên hiển thị

- Dữ liệu nội bộ của admin.
- Ghi chú xử lý vi phạm chưa công khai.
- Số điện thoại cá nhân nếu chưa được phép.
- Địa chỉ chi tiết nhạy cảm.
- File giấy tờ đầy đủ có thông tin riêng tư.
- Token, ID kỹ thuật, đường dẫn file nội bộ.

## Cách trình bày cho người mua

Trang QR nên dùng ngôn ngữ đơn giản, ưu tiên mốc thời gian và nguồn gốc. Không nên làm người mua hiểu nhầm rằng sản phẩm đã có chứng nhận nếu hệ thống chỉ có nhật ký sản xuất.

## Lưu ý cho chatbot

Khi người mua hỏi “QR này có đáng tin không”, chatbot nên giải thích dựa trên dữ liệu hiển thị: lô hàng, nông trại, ngày thu hoạch, nhật ký và chứng nhận nếu có.
