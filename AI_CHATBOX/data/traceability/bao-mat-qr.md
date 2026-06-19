---
doc_id: traceability__bao_mat_qr
title: Bảo mật QR truy xuất
category: traceability
audience: admin
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [traceability, qr, bao-mat]
source_type: internal_synthesis
review_required: true
---

# Bảo mật QR truy xuất

## Mục đích

Đưa ra nguyên tắc bảo mật khi tạo và hiển thị QR truy xuất nguồn gốc trong FarmTrace.

## Nguy cơ thường gặp

- QR bị sao chép sang sản phẩm khác.
- QR chứa dữ liệu nội bộ hoặc token bí mật.
- QR vẫn hoạt động sau khi lô hàng bị hủy.
- Người dùng đoán được mã lô hàng nếu mã quá đơn giản.
- Trang QR hiển thị thông tin cá nhân hoặc giấy tờ nhạy cảm.

## Nguyên tắc thiết kế QR

QR nên chứa URL công khai hoặc mã truy xuất, không chứa toàn bộ dữ liệu sản xuất. Khi người mua quét, hệ thống lấy dữ liệu công khai từ server và kiểm tra trạng thái QR tại thời điểm truy cập.

Mã truy xuất nên đủ khó đoán và không phụ thuộc trực tiếp vào ID tuần tự nội bộ. Nếu cần, có thể dùng mã công khai riêng thay vì dùng database ID.

## Kiểm soát hiển thị

- Chỉ hiển thị dữ liệu đã được phép công khai.
- Không hiển thị thông tin nhạy cảm của nông dân.
- Không hiển thị ghi chú admin nội bộ.
- Không hiển thị chứng nhận nếu chưa xác minh.
- Ghi log lượt quét nếu hệ thống cần theo dõi bất thường.

## Xử lý QR có vấn đề

Nếu QR bị sai dữ liệu, admin nên tạm dừng QR, ghi lý do, sửa lô hàng hoặc thông tin công khai, sau đó quét thử trước khi kích hoạt lại.

## Câu hỏi thường gặp

### Có nên nhúng toàn bộ nhật ký vào QR không?

Không nên. QR chỉ nên là cổng truy cập. Dữ liệu nên được quản lý trên hệ thống để có thể cập nhật, thu hồi hoặc kiểm soát hiển thị.
