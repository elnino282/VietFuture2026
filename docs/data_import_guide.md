# Hướng dẫn Reset và Import Dữ liệu An toàn

Tài liệu này hướng dẫn cách làm sạch dữ liệu trong Database một cách triệt để mà không làm hỏng cấu trúc do Flyway quản lý, đồng thời giải quyết triệt để lỗi ID (Khóa chính) liên tục tăng.

## 1. Nguyên tắc cốt lõi

*   **Tuyệt đối không dùng `DELETE` để reset data**: Dùng `DELETE` chỉ xóa row mà không reset bộ đếm `AUTO_INCREMENT`. Khi bạn import lại, ID sẽ nhảy số.
*   **Sử dụng `TRUNCATE`**: Lệnh `TRUNCATE TABLE` sẽ dọn sạch dữ liệu siêu tốc và reset bộ đếm ID về 1.
*   **Vô hiệu hóa Khóa ngoại (Foreign Key Checks)**: Khi dùng `TRUNCATE`, nếu các bảng có quan hệ cha-con, MySQL sẽ báo lỗi. Bạn cần chạy `SET FOREIGN_KEY_CHECKS = 0;` trước khi thực hiện.
*   **Né bảng `flyway_schema_history`**: Nếu bạn xóa dữ liệu bảng này, Flyway sẽ cố gắng chạy lại các file SQL `.sql` đã chạy từ trước, gây lỗi `Table already exists`.

## 2. Cài đặt
.\venv\Scripts\python.exe import_all_data.py

1. Tạo môi trường ảo (ở ngay trong thư mục project của bạn):

Bash
python -m venv venv
2. Kích hoạt môi trường ảo:

Bash
.\venv\Scripts\activate
(Bạn sẽ thấy chữ (venv) xuất hiện ở đầu dòng lệnh terminal của Zsh).

3. Cài đặt thư viện an toàn:
Lúc này, bạn đã nằm trong không gian an toàn của project, lệnh pip sẽ hoạt động bình thường mà không bị Arch Linux chặn:

Bash
pip install mysql-connector-python

## 3. Cách chạy Script

Script `import_all_data.py` đã được tạo sẵn để thực hiện 2 việc:
1. Quét toàn bộ Database, tự động `TRUNCATE` mọi bảng (chừa lại bảng của Flyway).
2. Tạo bộ dữ liệu mẫu nghiệp vụ **Quản lý Mùa vụ Nông nghiệp** (Seasons & Tasks) cực kỳ chuẩn xác và đầy đủ các trường.

Chạy script bằng lệnh:
```bash
python import_all_data.py
```


check data 

docker exec -it mysql_quanlymuavu mysql -uroot -prootpass

USE identity_db;

SELECT * FROM users LIMIT 10;