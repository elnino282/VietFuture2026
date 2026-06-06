# Import `newimportdb_final.sql`

Flow này dùng cho hướng Entity-first: Hibernate tạo schema từ Entity, sau đó SQL chỉ nạp dữ liệu seed.

## 1. Reset database rỗng

Chạy từ root repo:

```powershell
mysql -h localhost -P 3306 -u root -p --protocol=TCP < .\reset-database.sql
```
Hoặc chạy script sql file reset-database.sql qua console 1

## 2. Chạy backend profile `dev-reset`

```powershell
cd agricultural-crop-management-backend

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev-reset" "-Dspring-boot.run.arguments=--dev.bootstrap-admin.enabled=true --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=create --spring.sql.init.mode=never"
```

Dừng backend sau khi schema và tài khoản mặc định đã được tạo.

## 3. Import dữ liệu seed

```powershell
mysql -h localhost -P 3306 -u root -p --protocol=TCP quanlymuavu < .\newimportdb_final.sql
```

Hoặc chạy script sql file newimportdb_final.sql qua console 2

Chạy toàn bộ file từ đầu. Phần đầu file tự xử lý `SQL_SAFE_UPDATES` theo session và cleanup dữ liệu seed trước khi nạp lại.

## 4. Chạy smoke check (Không chạy cũng được)

Từ root repo:

```powershell
mysql -h localhost -P 3306 -u root -p --protocol=TCP quanlymuavu < .\scripts\sql\check-newimportdb-final.sql
```

## 5. Chạy backend profile `dev`

```powershell
cd agricultural-crop-management-backend

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.arguments=--spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=validate --spring.sql.init.mode=never"
```

## Seed login accounts

Các tài khoản đăng nhập chính do `ApplicationInitConfig` tạo:

- `admin@acm.local` / `admin123`
- `farmer@acm.local` / `12345678`
- `employee@acm.local` / `12345678`
- `buyer@acm.local` / `12345678`

Các actor phụ do seed tạo thêm dùng password `Password@123`:

- `farmer.binhminh@example.com`
- `employee.lanthao@example.com`
- `buyer.thucphamantoan@example.com`

## Manual checks

- Đăng nhập từng role chính: Admin, Farmer, Employee, Buyer.
- Farmer kiểm tra farm, plot, season, task, field log, expense, incident, harvest.
- Farmer kiểm tra kho vật tư và kho thành phẩm.
- Buyer xem marketplace, giỏ hàng, đơn hàng, đánh giá và truy xuất nguồn gốc.
- Employee xem task được giao, progress log và payroll.
- Admin xem user, report, marketplace payment verification, notification và audit log.
- Tạo mới Farm/Plot/Season qua app để xác nhận auto-increment vẫn hoạt động bình thường.
