---
doc_id: vietgap__loi-thuong-gap
title: Lỗi thường gặp khi áp dụng VietGAP và cách khắc phục
category: vietgap
audience: all
language: vi
version: 1.0
updated_at: 2026-06-19
tags: [vietgap, loi-thuong-gap, khac-phuc, kiem-tra-noi-bo, rui-ro]
source_refs: [src_tcvn_11892_vsqi, src_tcvn_11892_tcvn]
---

# Lỗi thường gặp khi áp dụng VietGAP và cách khắc phục

## Mục đích
Tài liệu này giúp nông dân, hợp tác xã và quản trị viên FarmTrace nhận diện các lỗi thường gặp khi áp dụng VietGAP.

Mục tiêu là hỗ trợ tự kiểm tra sớm, khắc phục sớm và giảm rủi ro khi đánh giá hồ sơ hoặc truy xuất nguồn gốc.

## Đối tượng sử dụng
- Nông dân tự kiểm tra tại vườn.
- Ban chủ nhiệm hợp tác xã.
- Người phụ trách chất lượng nội bộ.
- Quản trị viên FarmTrace.

## Nhóm lỗi 1: Ghi chép hồ sơ không đầy đủ
### Dấu hiệu
- Không có nhật ký sản xuất theo ngày.
- Nhật ký thiếu lô đất, thiếu người thực hiện hoặc thiếu liều lượng vật tư.
- Ghi dồn nhiều ngày cùng lúc.
- Hồ sơ vật tư bị mất hóa đơn, mất ảnh bao bì hoặc thiếu tên sản phẩm.

### Rủi ro
- Không chứng minh được quá trình sản xuất.
- Không truy xuất được nguyên nhân khi có sự cố.
- Dữ liệu trên QR không đủ độ tin cậy.

### Cách khắc phục
- Ghi nhật ký ngay sau khi thực hiện công việc.
- Dùng biểu mẫu ngắn, dễ điền.
- Phân công rõ người ghi và người kiểm tra.
- Dùng FarmTrace để lưu timestamp, ảnh chứng từ và lịch sử chỉnh sửa.

## Nhóm lỗi 2: Không kiểm soát thời gian cách ly
### Dấu hiệu
- Có phun thuốc BVTV nhưng không ghi thời gian cách ly.
- Tạo lệnh thu hoạch khi chưa kiểm tra lần phun thuốc gần nhất.
- Không biết lô nào đang trong giai đoạn chờ cách ly.

### Rủi ro
- Tăng nguy cơ dư lượng thuốc BVTV trên nông sản.
- Lô hàng có thể bị từ chối, thu hồi hoặc mất uy tín.

### Cách khắc phục
- Mỗi nhật ký thuốc BVTV phải ghi ngày phun và thời gian cách ly nếu có.
- Trước thu hoạch, kiểm tra nhật ký thuốc gần nhất.
- Thiết lập cảnh báo trên FarmTrace cho các lô chưa hết thời gian cách ly.

## Nhóm lỗi 3: Vật tư đầu vào không rõ nguồn gốc
### Dấu hiệu
- Mua giống, phân bón hoặc thuốc BVTV không có nhãn mác rõ.
- Không giữ hóa đơn hoặc ảnh bao bì.
- Dùng vật tư theo lời truyền miệng mà không có hướng dẫn.

### Rủi ro
- Không truy xuất được nguồn gốc vật tư.
- Khó chứng minh tính phù hợp khi kiểm tra.
- Có thể dùng nhầm sản phẩm không phù hợp hoặc không được phép.

### Cách khắc phục
- Lưu hóa đơn hoặc ảnh nhãn sản phẩm ngay khi mua.
- Tạo danh mục vật tư đã được hợp tác xã phê duyệt.
- Không cho nhập vật tư thiếu tên hoặc thiếu nguồn gốc vào FarmTrace.

## Nhóm lỗi 4: Kho và nơi chứa hóa chất không an toàn
### Dấu hiệu
- Để thuốc BVTV gần bếp, nhà ở, thực phẩm hoặc thức ăn chăn nuôi.
- Để thuốc, phân bón và dụng cụ thu hoạch chung một chỗ.
- Không có khóa, biển cảnh báo hoặc khu vực riêng.

### Rủi ro
- Nguy cơ ngộ độc cho người lao động và gia đình.
- Nguy cơ nhiễm chéo vào nông sản hoặc dụng cụ sản xuất.
- Bị đánh giá là điểm rủi ro nghiêm trọng trong quản lý an toàn.

### Cách khắc phục
- Tách riêng khu chứa thuốc BVTV.
- Có khóa hoặc người phụ trách quản lý.
- Không để dụng cụ thu hoạch chung với hóa chất.
- Ghi nhận kiểm tra kho trong checklist nội bộ.

## Nhóm lỗi 5: Thu hoạch và sơ chế gây nhiễm chéo
### Dấu hiệu
- Đặt nông sản trực tiếp xuống đất.
- Dùng bao bì phân bón hoặc hóa chất để đựng nông sản.
- Rửa nông sản bằng nguồn nước không an toàn.
- Để hàng sạch lẫn hàng bẩn hoặc rác thải.

### Rủi ro
- Sản phẩm có thể nhiễm bẩn sau khi đã sản xuất đúng quy trình.
- Khó đạt yêu cầu vệ sinh trong sơ chế, đóng gói và bán hàng.

### Cách khắc phục
- Dùng khay, sọt, bạt hoặc pallet sạch.
- Tách khu hàng thô và hàng đã làm sạch.
- Vệ sinh dụng cụ trước và sau sử dụng.
- Ghi nhật ký thu hoạch, sơ chế và nhập kho.

## Nhóm lỗi 6: Truy xuất nguồn gốc không khớp dữ liệu
### Dấu hiệu
- Sản lượng bán ra lớn bất thường so với diện tích hoặc sản lượng thu hoạch.
- QR không liên kết được với lô đất, mùa vụ hoặc batch.
- Trộn hàng nhiều nguồn nhưng không ghi tỷ lệ hoặc nguồn gốc.

### Rủi ro
- Người mua không tin vào mã QR.
- Hợp tác xã khó giải trình khi có khiếu nại.
- Có nguy cơ bị xem là gian lận truy xuất nguồn gốc.

### Cách khắc phục
- Mỗi sản phẩm bán ra cần liên kết với Batch ID.
- Batch ID cần liên kết với nhật ký thu hoạch và mùa vụ.
- Kiểm tra cân bằng sản lượng: thu hoạch, nhập kho, xuất kho và số tem QR.

## Bảng tổng hợp lỗi và cách xử lý
| Nhóm lỗi | Cách FarmTrace hỗ trợ |
|---|---|
| Thiếu nhật ký | Nhắc việc, bắt buộc trường dữ liệu tối thiểu |
| Ghi dồn | Lưu thời gian tạo và lịch sử chỉnh sửa |
| Thiếu chứng từ vật tư | Cho phép chụp ảnh hóa đơn, bao bì |
| Chưa hết cách ly | Cảnh báo trước thu hoạch |
| Nhiễm chéo sơ chế | Mini-checklist vệ sinh trước nhập kho |
| Truy xuất không khớp | Kiểm tra liên kết Farm ID, Plot ID, Season ID, Batch ID, QR |

## Checklist phòng tránh lỗi
- [ ] Không ghi nhật ký đối phó hoặc ghi khống.
- [ ] Mỗi lần dùng vật tư đều có nhật ký.
- [ ] Mỗi lần dùng thuốc BVTV đều kiểm tra thời gian cách ly.
- [ ] Vật tư có nguồn gốc rõ ràng.
- [ ] Kho thuốc tách biệt với khu sinh hoạt và nông sản.
- [ ] Dụng cụ thu hoạch và sơ chế được vệ sinh.
- [ ] Sản lượng thu hoạch, nhập kho và bán ra khớp logic.
- [ ] QR truy xuất liên kết đúng với lô hàng.

## Câu hỏi thường gặp

### Câu hỏi: Lỗi nào nghiêm trọng nhất khi làm VietGAP?
Trả lời: Không nên kết luận chung nếu chưa có bối cảnh đánh giá. Tuy nhiên, các lỗi liên quan đến sử dụng vật tư không phù hợp, không kiểm soát thời gian cách ly, gian lận hồ sơ hoặc mất truy xuất nguồn gốc đều là rủi ro lớn.

### Câu hỏi: Ghi thiếu nhật ký có thể bổ sung sau không?
Trả lời: Có thể bổ sung nếu thông tin là đúng thực tế và có ghi chú rõ. Không nên ghi khống hoặc làm hồ sơ giả.

### Câu hỏi: FarmTrace có phát hiện được mọi lỗi không?
Trả lời: Không. FarmTrace hỗ trợ phát hiện lỗi dữ liệu và quy trình, nhưng vẫn cần kiểm tra thực tế tại nông trại.

## Giới hạn áp dụng
Tài liệu này phục vụ tự kiểm tra và cải thiện dữ liệu. Kết luận đạt hoặc không đạt vẫn phụ thuộc vào quá trình đánh giá thực tế.
