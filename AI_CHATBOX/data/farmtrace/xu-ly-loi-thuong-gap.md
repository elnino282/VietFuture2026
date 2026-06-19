---
doc_id: farmtrace__xu_ly_loi_thuong_gap
title: Xử lý lỗi thường gặp trong FarmTrace
category: farmtrace
audience: all
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [farmtrace, loi-thuong-gap, ho-tro]
source_type: internal_synthesis
review_required: true
---

# Xử lý lỗi thường gặp trong FarmTrace

## Mục đích

Cung cấp hướng dẫn xử lý các lỗi nghiệp vụ phổ biến để chatbot hỗ trợ nông dân, admin và người mua nhanh hơn.

## Không tạo được mùa vụ

Nguyên nhân thường gặp là chưa có nông trại, thiếu cây trồng, thiếu ngày bắt đầu hoặc tài khoản không có quyền. Cách xử lý là tạo nông trại trước, kiểm tra thông tin bắt buộc và thử lưu lại.

## Không ghi được nhật ký

Người dùng cần chọn đúng mùa vụ và loại nhật ký. Nếu mùa vụ đã đóng hoặc đã bị hủy, hệ thống có thể không cho ghi thêm nhật ký.

## Không nhập kho được

Cần có phiếu thu hoạch hoặc mùa vụ đã đến giai đoạn thu hoạch. Sản lượng nhập kho không nên lớn hơn sản lượng thu hoạch nếu hệ thống có kiểm tra tồn.

## Không đăng bán được sản phẩm

Các nguyên nhân thường gặp gồm chưa có lô hàng, lô hàng hết tồn, sản phẩm bị admin giữ lại, thiếu giá bán hoặc thiếu đơn vị tính.

## QR không hiển thị dữ liệu

QR có thể chưa liên kết lô hàng, lô hàng bị hủy, thông tin công khai chưa được duyệt hoặc URL truy xuất không hợp lệ.

## Chứng nhận không hiển thị

Chứng nhận có thể đang chờ admin duyệt, đã hết hạn, không thuộc phạm vi lô hàng hoặc bị từ chối vì thiếu thông tin.

## Cách chatbot nên trả lời khi thiếu dữ liệu

Chatbot nên nói rõ bước cần kiểm tra tiếp theo, không phỏng đoán. Ví dụ: “Bạn hãy kiểm tra lô hàng đã nhập kho chưa” thay vì “hệ thống bị lỗi”.
