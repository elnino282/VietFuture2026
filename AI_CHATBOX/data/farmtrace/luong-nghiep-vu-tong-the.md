---
doc_id: farmtrace__luong_nghiep_vu_tong_the
title: Luồng nghiệp vụ tổng thể FarmTrace
category: farmtrace
audience: all
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [farmtrace, workflow, mvp]
source_type: internal_synthesis
review_required: true
---

# Luồng nghiệp vụ tổng thể FarmTrace

## Mục đích

Mô tả luồng MVP từ tạo nông trại đến QR truy xuất để chatbot trả lời thống nhất về quy trình FarmTrace.

## Luồng chính

1. Nông dân tạo nông trại.
2. Nông dân tạo mùa vụ và liên kết với nông trại.
3. Nông dân ghi nhật ký sản xuất theo từng giai đoạn.
4. Admin kiểm tra dữ liệu theo giai đoạn nếu quy trình yêu cầu.
5. Nông dân thu hoạch và tạo phiếu thu hoạch.
6. Nông dân nhập kho sau thu hoạch.
7. Hệ thống tạo hoặc cập nhật lô hàng.
8. Nông dân đăng bán sản phẩm từ lô hàng hợp lệ.
9. Hệ thống tạo QR truy xuất cho lô hàng hoặc sản phẩm.
10. Người mua quét QR và xem thông tin công khai.

## Dữ liệu liên kết quan trọng

| Đối tượng | Liên kết bắt buộc nên có |
|---|---|
| Nông trại | Chủ nông trại, khu vực, chứng nhận nếu có |
| Mùa vụ | Nông trại, cây trồng, diện tích, giai đoạn |
| Nhật ký | Mùa vụ, ngày thực hiện, loại hoạt động |
| Thu hoạch | Mùa vụ, sản lượng, ngày thu hoạch |
| Kho | Phiếu thu hoạch, sản lượng nhập, trạng thái |
| Sản phẩm | Lô hàng, giá bán, tồn kho |
| QR | Lô hàng, thông tin công khai, trạng thái hợp lệ |

## Trạng thái cần kiểm soát

- Mùa vụ mới tạo chưa có nhật ký thì chưa đủ dữ liệu truy xuất.
- Lô hàng chưa nhập kho thì không nên đăng bán như hàng sẵn có.
- QR chưa liên kết lô hàng thì không có giá trị truy xuất.
- Chứng nhận chưa được admin xác minh thì không nên hiển thị như chứng nhận hợp lệ.

## Lưu ý cho chatbot

Khi người dùng hỏi “làm bước nào trước”, chatbot nên ưu tiên trả lời theo thứ tự: nông trại → mùa vụ → nhật ký → thu hoạch → kho → đăng bán → QR.
