# FarmTrace - Nghiệp vụ MVP hỗ trợ VietGAP và truy xuất nguồn gốc

## 1. Mục tiêu của FarmTrace

FarmTrace là hệ thống hỗ trợ nông dân, trang trại và hợp tác xã quản lý quy trình sản xuất nông nghiệp theo hướng VietGAP, ghi nhật ký canh tác, quản lý thu hoạch, nhập kho, tạo QR truy xuất nguồn gốc và đăng bán sản phẩm trên sàn thương mại nông sản.

Mục tiêu chính:

* Giúp nông dân ghi chép quy trình sản xuất dễ dàng.
* Giúp admin hoặc cán bộ kiểm tra theo từng giai đoạn sản xuất.
* Giúp sản phẩm có dữ liệu truy xuất nguồn gốc rõ ràng.
* Giúp người mua kiểm tra thông tin sản phẩm qua mã QR.
* Giúp trang trại đăng bán sản phẩm sau khi thu hoạch và nhập kho.
* Giúp quản lý giấy tờ như giấy kinh doanh, chứng nhận VietGAP và hồ sơ liên quan.

## 2. Vai trò người dùng

### 2.1. Nông dân hoặc chủ trang trại

Nông dân tạo mùa vụ, ghi nhật ký sản xuất, cập nhật hoạt động chăm sóc, bón phân, phun thuốc, thu hoạch, nhập kho và đăng bán sản phẩm.

### 2.2. Admin hoặc cán bộ kiểm tra

Admin kiểm tra dữ liệu theo từng giai đoạn sản xuất. Nếu dữ liệu đạt yêu cầu, admin xác nhận. Nếu dữ liệu thiếu hoặc không hợp lệ, admin yêu cầu bổ sung hoặc chỉnh sửa.

### 2.3. Người mua

Người mua xem sản phẩm trên sàn thương mại, kiểm tra thông tin trang trại, xem QR truy xuất nguồn gốc và đặt hàng.

### 2.4. Người tiêu dùng quét QR

Người tiêu dùng quét QR để xem thông tin cơ bản về sản phẩm, trang trại, mùa vụ, nhật ký sản xuất, thu hoạch, đóng gói và chứng nhận nếu có.

## 3. Core module MVP

### Module 1: Quản lý quy trình sản xuất VietGAP

Đây là module quan trọng nhất của hệ thống. Nông dân cần tạo mùa vụ và cập nhật các giai đoạn sản xuất.

Quy trình sản xuất cơ bản:

1. Lập kế hoạch mùa vụ.
2. Chuẩn bị đất.
3. Gieo trồng.
4. Chăm sóc.
5. Tưới nước.
6. Bón phân.
7. Phun thuốc bảo vệ thực vật nếu cần.
8. Kiểm tra chất lượng.
9. Thu hoạch.
10. Đóng gói.
11. Nhập kho.
12. Đăng bán.

Mỗi giai đoạn cần có trạng thái để admin kiểm tra:

* Chưa bắt đầu.
* Đang thực hiện.
* Chờ kiểm tra.
* Cần bổ sung.
* Đã xác nhận.
* Hoàn thành.

### Module 2: Nhật ký sản xuất

Nhật ký sản xuất là dữ liệu nền tảng để chứng minh quy trình VietGAP và tạo truy xuất nguồn gốc.

Các loại nhật ký cần có:

* Nhật ký đất.
* Nhật ký giống.
* Nhật ký tưới nước.
* Nhật ký phân bón.
* Nhật ký thuốc bảo vệ thực vật.
* Nhật ký chăm sóc.
* Nhật ký kiểm tra chất lượng.
* Nhật ký thu hoạch.
* Nhật ký đóng gói.
* Nhật ký nhập kho.
* Nhật ký bán hàng.

Mỗi nhật ký nên có:

* Mã mùa vụ.
* Mã lô sản xuất.
* Ngày thực hiện.
* Nội dung công việc.
* Người thực hiện.
* Vật tư sử dụng nếu có.
* Số lượng hoặc liều lượng.
* Hình ảnh minh chứng nếu có.
* Ghi chú.
* Trạng thái kiểm tra.

### Module 3: Admin kiểm tra theo thời gian thực

Admin không cần đợi kết thúc mùa vụ mới kiểm tra. Hệ thống cho phép admin kiểm tra từng giai đoạn.

Luồng kiểm tra:

1. Nông dân cập nhật nhật ký cho một giai đoạn.
2. Hệ thống chuyển trạng thái giai đoạn thành “Chờ kiểm tra”.
3. Admin xem thông tin, ảnh minh chứng và vật tư đã sử dụng.
4. Nếu hợp lệ, admin xác nhận.
5. Nếu thiếu dữ liệu, admin yêu cầu bổ sung.
6. Nông dân chỉnh sửa hoặc bổ sung minh chứng.
7. Admin kiểm tra lại.

### Module 4: Thu hoạch và nhập kho

Sau khi thu hoạch, sản phẩm không nên chuyển thẳng sang đăng bán. Sản phẩm cần được ghi nhận vào kho trước.

Luồng thu hoạch:

1. Nông dân tạo bản ghi thu hoạch.
2. Nhập ngày thu hoạch, sản lượng, khu vực thu hoạch, lô sản xuất.
3. Phân loại sản phẩm nếu cần.
4. Đóng gói sản phẩm.
5. Nhập kho.
6. Hệ thống tạo tồn kho theo sản phẩm và lô.
7. Chỉ sản phẩm có tồn kho hợp lệ mới được đăng bán.

Thông tin nhập kho cần có:

* Mã sản phẩm.
* Mã lô sản xuất.
* Mã mùa vụ.
* Số lượng nhập kho.
* Đơn vị tính.
* Ngày nhập kho.
* Hạn sử dụng nếu có.
* Trạng thái chất lượng.
* Vị trí kho nếu có.

### Module 5: Truy xuất nguồn gốc QR

QR truy xuất nguồn gốc giúp người mua kiểm tra lịch sử sản phẩm.

QR nên hiển thị:

* Tên sản phẩm.
* Tên trang trại.
* Địa chỉ hoặc khu vực sản xuất.
* Mã lô sản xuất.
* Mã mùa vụ.
* Ngày gieo trồng.
* Ngày thu hoạch.
* Quy trình chăm sóc tóm tắt.
* Nhật ký phân bón.
* Nhật ký thuốc bảo vệ thực vật.
* Thông tin đóng gói.
* Thông tin nhập kho.
* Chứng nhận nếu có.
* Trạng thái xác thực QR.

QR cần có bảo mật:

* Mỗi QR gắn với một mã lô hoặc mã sản phẩm duy nhất.
* QR có đường dẫn xác thực từ hệ thống.
* QR có trạng thái: hoạt động, hết hạn, bị thu hồi.
* QR không nên cho phép sửa dữ liệu sau khi đã công bố nếu không có lịch sử chỉnh sửa.
* Nếu dữ liệu bị thay đổi, hệ thống cần ghi nhận người sửa và thời gian sửa.

### Module 6: Upload giấy tờ và chứng nhận

Trang trại có thể upload:

* Giấy đăng ký kinh doanh.
* Giấy chứng nhận VietGAP.
* Giấy chứng nhận an toàn thực phẩm nếu có.
* Hóa đơn mua giống.
* Hóa đơn mua phân bón.
* Hóa đơn mua thuốc bảo vệ thực vật.
* Tài liệu kiểm nghiệm chất lượng nếu có.

Trạng thái giấy tờ:

* Chờ duyệt.
* Hợp lệ.
* Cần bổ sung.
* Hết hạn.
* Bị từ chối.

### Module 7: Đăng bán sản phẩm

Chỉ nên cho phép đăng bán khi:

* Sản phẩm đã có thông tin trang trại.
* Có lô sản xuất rõ ràng.
* Đã thu hoạch.
* Đã nhập kho.
* Có số lượng tồn kho.
* Có thông tin truy xuất nguồn gốc.
* Không bị admin chặn do thiếu dữ liệu nghiêm trọng.

Thông tin sản phẩm đăng bán:

* Tên sản phẩm.
* Hình ảnh.
* Giá bán.
* Đơn vị tính.
* Số lượng tồn kho.
* Trang trại bán.
* Mô tả sản phẩm.
* QR truy xuất nguồn gốc.
* Chứng nhận nếu có.
* Chính sách giao hàng.

### Module 8: AI Chatbox

AI Chatbox hỗ trợ trả lời các câu hỏi liên quan đến:

* VietGAP là gì.
* Cần ghi nhật ký nào để đạt VietGAP.
* Quy trình sản xuất gồm những bước nào.
* FarmTrace hỗ trợ truy xuất nguồn gốc ra sao.
* QR hiển thị thông tin gì.
* Khi nào sản phẩm được đăng bán.
* Tại sao cần nhập kho sau thu hoạch.
* Admin kiểm tra giai đoạn sản xuất như thế nào.
* Trang trại cần upload giấy tờ gì.

AI Chatbox không nên tự bịa thông tin ngoài tài liệu. Nếu chưa có dữ liệu, bot cần trả lời rằng tài liệu hiện tại chưa đủ thông tin.

## 4. Luồng nghiệp vụ MVP tổng quát

Nông dân tạo mùa vụ.
Sau đó nông dân ghi nhật ký sản xuất theo từng giai đoạn.
Admin kiểm tra giai đoạn theo thời gian thực.
Khi hoàn tất sản xuất, nông dân tạo bản ghi thu hoạch.
Sản phẩm sau thu hoạch được đóng gói và nhập kho.
Hệ thống tạo QR truy xuất nguồn gốc cho lô sản phẩm.
Nông dân đăng bán sản phẩm có tồn kho trên sàn thương mại.
Người mua xem sản phẩm và quét QR để kiểm tra nguồn gốc.

## 5. Quy tắc nghiệp vụ quan trọng

* Không cho đăng bán sản phẩm chưa nhập kho.
* Không tạo QR nếu thiếu mã lô sản xuất.
* Không xác nhận giai đoạn nếu thiếu nhật ký bắt buộc.
* Không công bố chứng nhận nếu giấy tờ chưa được duyệt.
* Không cho sửa dữ liệu QR đã công bố mà không lưu lịch sử chỉnh sửa.
* Không cho bán vượt quá số lượng tồn kho.
* Không dùng AI Chatbox thay cho chứng nhận VietGAP chính thức.
