---
doc_id: farmtrace__vai_tro_nguoi_dung
title: Vai trò người dùng trong FarmTrace
category: farmtrace
audience: all
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [farmtrace, vai-tro, phan-quyen]
source_type: internal_synthesis
review_required: true
---

# Vai trò người dùng trong FarmTrace

## Mục đích

Làm rõ vai trò chính trong FarmTrace để chatbot trả lời đúng theo quyền và nhu cầu của từng nhóm người dùng.

## Nông dân hoặc chủ nông trại

Nông dân là người tạo nông trại, tạo mùa vụ, ghi nhật ký sản xuất, thu hoạch, nhập kho, đăng bán sản phẩm và tạo QR truy xuất nếu hệ thống cho phép.

Nông dân chịu trách nhiệm nhập dữ liệu đúng thực tế. Nếu thiếu nhật ký, hệ thống hoặc admin có thể yêu cầu bổ sung trước khi duyệt lô hàng hoặc hiển thị thông tin công khai.

## Admin

Admin kiểm tra dữ liệu, xác minh hồ sơ, duyệt giai đoạn, quản lý chứng nhận và xử lý lỗi nghiệp vụ. Admin không nên tự sửa dữ liệu sản xuất thay nông dân nếu không có ghi chú hoặc biên bản rõ ràng.

Admin có thể yêu cầu bổ sung hồ sơ, từ chối dữ liệu không hợp lệ hoặc xác nhận trạng thái chứng nhận nếu thông tin phù hợp.

## Người mua

Người mua là người xem sản phẩm, đặt mua và quét QR truy xuất. Người mua chỉ nên xem thông tin công khai như tên sản phẩm, nông trại, vùng sản xuất, ngày thu hoạch, lô hàng, trạng thái chứng nhận và nhật ký đã được công khai.

Người mua không nên thấy dữ liệu nội bộ, thông tin nhạy cảm, số điện thoại riêng, giấy tờ đầy đủ hoặc log kiểm tra của admin.

## Nguyên tắc phân quyền

- Người tạo dữ liệu chịu trách nhiệm về dữ liệu.
- Admin kiểm tra và xác minh, không thay thế toàn bộ quy trình sản xuất.
- Người mua chỉ xem dữ liệu công khai.
- Chatbot không hướng dẫn người dùng vượt quyền hoặc xem dữ liệu không thuộc phạm vi.
