# Database Reset Empty (Entity-First)

## Flow mới: reset DB trống

Chạy từ thư mục gốc repo:

```powershell
.\scripts\db-reset-empty.ps1 `
  -DatabaseName quanlymuavu `
  -MySqlUser root `
  -MySqlPassword "your-password" `
  -MySqlHost localhost `
  -MySqlPort 3306
```

Sau đó start backend để Hibernate tạo schema từ entity:

```powershell
cd agricultural-crop-management-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev-reset
```

Nếu restart mà không muốn mất data vừa nhập:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Smoke check schema

```powershell
mysql --host=localhost --port=3306 --user=root --password=your-password --protocol=TCP --default-character-set=utf8mb4 --database=quanlymuavu --execute="SOURCE scripts/sql/check-hibernate-schema.sql"
```

Kỳ vọng:

- Query `flyway_history_count` trả về `0`.
- Query AUTO_INCREMENT trả về `0 rows`.

## Anti-pattern mới

- Không bật lại Flyway trong local/dev flow mới.
- Không import migration SQL cũ.
- Không import full dump schema.
- Không dùng `ddl-auto=update` làm chuẩn.
- Không chạy profile `dev-reset` nếu muốn giữ data đã nhập thủ công.
- Không auto-seed (`data.sql`, `schema.sql`, `newimportdb.sql`) trong flow mặc định.

## Ghi chú manual test với DB trống

- Hệ thống không tự bootstrap demo data.
- Nếu cần user đầu tiên, dùng signup flow hiện tại.
- `DEV_BOOTSTRAP_ADMIN_ENABLED=false` trong `.env.example` là mặc định an toàn (không tự tạo admin).

