# 🐧 Hướng dẫn Khởi chạy Project trên Linux

> Tài liệu hướng dẫn **từng bước** để chạy thành công toàn bộ hệ thống ACM (Agricultural Crop Management) trên Linux, bao gồm kiểm tra môi trường, khởi chạy Backend (Microservices), Frontend (React + Vite), và **giải thích cơ chế tự động import dữ liệu** của Flyway.

---

## 📋 Mục lục

1. [Yêu cầu hệ thống](#-1-yêu-cầu-hệ-thống)
2. [Kiểm tra môi trường](#-2-kiểm-tra-môi-trường)
3. [Chuẩn bị cấu hình](#-3-chuẩn-bị-cấu-hình)
4. [Khởi chạy Backend](#-4-khởi-chạy-backend-docker)
5. [Xác nhận Backend chạy thành công](#-5-xác-nhận-backend-chạy-thành-công)
6. [Khởi chạy Frontend](#-6-khởi-chạy-frontend-react--vite)
7. [Giải thích cơ chế Import dữ liệu (Flyway)](#-7-giải-thích-cơ-chế-import-dữ-liệu-flyway)
8. [Tài khoản đăng nhập mẫu](#-8-tài-khoản-đăng-nhập-mẫu)
9. [Danh sách cổng dịch vụ](#-9-danh-sách-cổng-dịch-vụ)
10. [Xử lý sự cố (Troubleshooting)](#-10-xử-lý-sự-cố-troubleshooting)

---

## 🔧 1. Yêu cầu hệ thống

| Phần mềm | Phiên bản tối thiểu | Mục đích |
|---|---|---|
| **Docker** | 20.10+ | Chạy MySQL, RabbitMQ, MinIO, microservices |
| **Docker Compose** | v2.0+ (plugin) | Orchestrate toàn bộ containers |
| **Node.js** | v18+ | Chạy Frontend React dev server |
| **npm** | v9+ | Quản lý dependencies Frontend |
| **Git** | 2.x | Clone và đồng bộ mã nguồn |
| **OpenSSL** | 1.1+ | Generate RSA key pair cho JWT |

---

## ✅ 2. Kiểm tra môi trường

Chạy lần lượt các lệnh sau để đảm bảo máy đã cài đủ công cụ:

### 2.1. Kiểm tra Docker

```bash
docker --version
# ✅ Kỳ vọng: Docker version 20.10+ hoặc mới hơn

docker compose version
# ✅ Kỳ vọng: Docker Compose version v2.x+
```

Kiểm tra Docker daemon đang chạy:

```bash
docker info > /dev/null 2>&1 && echo "✅ Docker daemon đang chạy" || echo "❌ Docker daemon chưa chạy"
```

> Nếu Docker daemon chưa chạy, khởi động bằng:
> ```bash
> sudo systemctl start docker
> ```

### 2.2. Kiểm tra Node.js & npm

```bash
node --version
# ✅ Kỳ vọng: v18.x hoặc mới hơn

npm --version
# ✅ Kỳ vọng: 9.x hoặc mới hơn
```

### 2.3. Kiểm tra Git

```bash
git --version
# ✅ Kỳ vọng: git version 2.x
```

### 2.4. Kiểm tra OpenSSL

```bash
openssl version
# ✅ Kỳ vọng: OpenSSL 1.1+ hoặc 3.x
```

### 2.5. Kiểm tra cổng không bị chiếm

Các cổng sau **bắt buộc phải trống** trước khi chạy:

```bash
# Kiểm tra nhanh tất cả cổng cần dùng
for port in 3307 5672 15672 9000 9001 1025 8025 8000 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091 9090 3001 5173; do
  if ss -tlnp | grep -q ":$port "; then
    echo "❌ Cổng $port đang bị chiếm!"
  else
    echo "✅ Cổng $port trống"
  fi
done
```

> Nếu cổng nào bị chiếm, tìm và dừng process đang dùng:
> ```bash
> sudo lsof -i :PORT_NUMBER
> # hoặc
> sudo fuser -k PORT_NUMBER/tcp
> ```

---

## ⚙️ 3. Chuẩn bị cấu hình

### 3.1. Clone project (nếu chưa có)

```bash
git clone <URL_REPOSITORY> VietFuture2026
cd VietFuture2026
```

### 3.2. Tạo file `.env` cho Backend

```bash
cp .env.example .env
```

Mở file `.env` và cập nhật các giá trị cần thiết:

```bash
nano .env
```

Các giá trị **quan trọng cần cập nhật**:

```properties
# (Bắt buộc nếu muốn dùng AI chatbot)
GEMINI_API_KEY=your_gemini_api_key_here

# (Tuỳ chọn - Đăng nhập bằng Google)
GOOGLE_CLIENT_ID=your_google_client_id
```

> **Lưu ý:** Các giá trị MySQL, RabbitMQ, MinIO đã có default trong `.env.example`, không cần thay đổi nếu chạy local.

### 3.3. Generate RSA Key Pair cho JWT (Bắt buộc)

Identity-service sử dụng RSA (RS256) để ký JWT. File `.pem` bị `.gitignore` nên **mỗi máy phải tự generate**:

```bash
# Tạo thư mục chứa keys
mkdir -p identity-service/src/main/resources/keys

# Generate private key (RSA 2048-bit, PKCS#8)
openssl genpkey -algorithm RSA \
  -out identity-service/src/main/resources/keys/rsa-private.pem \
  -pkeyopt rsa_keygen_bits:2048

# Trích xuất public key
openssl rsa -pubout \
  -in identity-service/src/main/resources/keys/rsa-private.pem \
  -out identity-service/src/main/resources/keys/rsa-public.pem
```

Xác nhận keys đã tạo:

```bash
ls -la identity-service/src/main/resources/keys/
# ✅ Kỳ vọng: rsa-private.pem và rsa-public.pem
```

---

## 🐳 4. Khởi chạy Backend (Docker)

### 4.1. Build và khởi động toàn bộ services

```bash
docker compose up -d --build season-service
```
mvn clean package -DskipTests
> **Lần đầu tiên** sẽ mất **5–15 phút** để download Docker images và build Maven (tải dependencies). Các lần sau sẽ nhanh hơn nhiều nhờ Docker cache.

### 4.2. Theo dõi quá trình khởi động

```bash
# Xem trạng thái realtime
docker compose ps

# Hoặc theo dõi logs toàn bộ
docker compose logs identity-service -f --tail=50
```

Đợi khoảng **60–90 giây** sau khi tất cả containers đã `Started` để các Spring Boot services hoàn tất khởi tạo và Flyway chạy migrations.

---

## 🔍 5. Xác nhận Backend chạy thành công

### 5.1. Kiểm tra tất cả containers đang chạy ổn định

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

**✅ Kết quả đúng**: Tất cả containers phải hiển thị `Up X minutes` (không phải `Restarting`):

```
NAMES                                 STATUS
api_gateway_quanlymuavu               Up 5 minutes
identity_service_quanlymuavu          Up 5 minutes
crop_catalog_service_quanlymuavu      Up 5 minutes
farm_service_quanlymuavu              Up 5 minutes
season_service_quanlymuavu            Up 5 minutes
inventory_service_quanlymuavu         Up 5 minutes
finance_service_quanlymuavu           Up 5 minutes
incident_service_quanlymuavu          Up 5 minutes
sustainability_service_quanlymuavu    Up 5 minutes
marketplace_service_quanlymuavu       Up 5 minutes
admin_reporting_service_quanlymuavu   Up 5 minutes (healthy)
ai_service_quanlymuavu                Up 5 minutes
mysql_quanlymuavu                     Up 5 minutes (healthy)
rabbitmq_quanlymuavu                  Up 5 minutes (healthy)
minio_quanlymuavu                     Up 5 minutes (healthy)
...
```

> ⚠️ Nếu thấy service nào hiển thị `Restarting`, xem mục [Xử lý sự cố](#-10-xử-lý-sự-cố-troubleshooting).

### 5.2. Kiểm tra MySQL — Đã tạo đủ databases

```bash
docker exec mysql_quanlymuavu mysql -uroot -prootpass -e "SHOW DATABASES;" 2>/dev/null
```

**✅ Kết quả đúng** — phải có đủ **10 databases** (ngoài system databases):

```
Database
admin_reporting_db
crop_catalog_db
farm_db
finance_db
identity_db
incident_db
inventory_db
marketplace_db
season_db
sustainability_db
```

### 5.3. Kiểm tra Flyway đã tạo tables

```bash
# Đếm số tables trong mỗi database
docker exec mysql_quanlymuavu mysql -uroot -prootpass -e "
SELECT table_schema AS 'Database', COUNT(*) AS 'Tables'
FROM information_schema.tables
WHERE table_schema IN (
  'identity_db','crop_catalog_db','farm_db','season_db',
  'inventory_db','finance_db','incident_db','sustainability_db',
  'marketplace_db','admin_reporting_db'
)
GROUP BY table_schema
ORDER BY table_schema;" 2>/dev/null
```

**✅ Kết quả đúng** — mỗi database phải có ít nhất 3+ tables:

```
Database              Tables
admin_reporting_db    17
crop_catalog_db       5
farm_db               6
finance_db            3
identity_db           8
incident_db           6
inventory_db          13
marketplace_db        13
season_db             11
sustainability_db     13
```

Import data theo hướng dẫn 

### 5.4. Kiểm tra quyền user MySQL

```bash
docker exec mysql_quanlymuavu mysql -uroot -prootpass -e "SHOW GRANTS FOR 'springuser'@'%';" 2>/dev/null
```

**✅ Kết quả đúng** — `springuser` phải có `ALL PRIVILEGES` trên mỗi database.

### 5.5. Kiểm tra API Gateway hoạt động

```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost:8000/actuator/health
# ✅ Kỳ vọng: HTTP Status: 200
```

### 5.6. Kiểm tra Identity-service (JWT + Flyway)

```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost:8081/actuator/health
# ✅ Kỳ vọng: HTTP Status: 200
```

---

## 🖥️ 6. Khởi chạy Frontend (React + Vite)

### 6.1. Mở terminal mới, di chuyển vào thư mục frontend

```bash
cd agricultural-crop-management-frontend
```

### 6.2. Cài đặt dependencies

```bash
npm install
```

> Lần đầu mất khoảng 1–3 phút để tải packages.

### 6.3. Khởi chạy dev server

```bash
npm run dev
```

**✅ Kết quả đúng:**

```
  VITE v6.x.x  ready in xxx ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: http://192.168.x.x:5173/
```

### 6.4. Kiểm tra Frontend đang chạy

Mở trình duyệt và truy cập:

```
http://localhost:5173
```

> **Lưu ý:** Frontend kết nối đến API Gateway tại `http://localhost:8000` (đã cấu hình sẵn trong `.env.development`). Đảm bảo Backend đang chạy trước khi mở Frontend.

---

## 📦 7. Giải thích cơ chế Import dữ liệu (Flyway)

### Flyway là gì và hoạt động như thế nào?

**Flyway** là công cụ database migration tích hợp sẵn trong mỗi Spring Boot service. Khi service khởi động, Flyway **tự động** chạy các file SQL migration để tạo schema (tables, indexes, constraints).

### Luồng hoạt động khi chạy `docker compose up`:

```
1. MySQL container khởi động
   └─→ Chạy init-mysql.sql (tạo 10 databases + GRANT quyền)

2. MySQL trở thành "healthy"

3. Các Spring Boot services khởi động
   └─→ Mỗi service tự chạy Flyway migration:
       ├── identity-service  → V1__init.sql, V2__add_outbox_events.sql
       ├── farm-service      → V1__init.sql
       ├── season-service    → V1__init.sql → V2 → ... → V9
       ├── inventory-service → V1__init.sql → V2 → V3
       └── ... (tương tự cho các service khác)

4. Hibernate validate schema → Nếu Flyway đã tạo đúng tables → Service khởi động thành công
```

### ⚠️ QUAN TRỌNG: Phân biệt Schema vs Seed Data

| | Schema (Cấu trúc bảng) | Seed Data (Dữ liệu mẫu) |
|---|---|---|
| **Được tạo bởi** | Flyway — **TỰ ĐỘNG** khi service khởi động | Script import — **THỦ CÔNG** |
| **Gồm những gì** | CREATE TABLE, ALTER TABLE, CREATE INDEX | INSERT INTO (users, farms, crops, ...) |
| **Khi nào cần chạy** | Không cần làm gì, Flyway lo hoàn toàn | Cần chạy script sau khi Backend lên thành công |
| **File nguồn** | `*/src/main/resources/db/migration/V*.sql` | Xem hướng dẫn import data bên dưới |

### Kết luận

> **Flyway CHỈ tạo cấu trúc bảng (schema), KHÔNG tự import dữ liệu mẫu (seed data).**
>
> Sau khi chạy `docker compose up` thành công, các databases sẽ có đầy đủ tables nhưng **hoàn toàn trống** (không có dữ liệu). Để có dữ liệu mẫu trải nghiệm, bạn cần chạy script import thủ công.

### Cách kiểm tra Flyway đã chạy thành công

```bash
# Xem Flyway history trong bất kỳ database nào
docker exec mysql_quanlymuavu mysql -uroot -prootpass -e \
  "SELECT version, description, installed_on, success
   FROM identity_db.flyway_schema_history
   ORDER BY installed_rank;" 2>/dev/null
```

**✅ Kết quả đúng:**

```
version | description              | installed_on        | success
0       | << Flyway Baseline >>    | 2026-06-30 09:00:00 | 1
1       | initial identity schema  | 2026-06-30 09:00:01 | 1
2       | add outbox events        | 2026-06-30 09:00:01 | 1
```

### Nơi chứa file migration SQL của từng service

| Service | Thư mục migration |
|---|---|
| identity-service | `identity-service/src/main/resources/db/migration/` |
| crop-catalog-service | `crop-catalog-service/src/main/resources/db/migration/` |
| farm-service | `farm-service/src/main/resources/db/migration/` |
| season-service | `season-service/src/main/resources/db/migration/` |
| inventory-service | `inventory-service/src/main/resources/db/migration/` |
| finance-service | `finance-service/src/main/resources/db/migration/` |
| incident-service | `incident-service/src/main/resources/db/migration/` |
| sustainability-service | `sustainability-service/src/main/resources/db/migration/` |
| marketplace-service | `marketplace-service/src/main/resources/db/migration/` |
| admin-reporting-service | `admin-reporting-service/src/main/resources/db/migration/` |

---

## 🔑 8. Tài khoản đăng nhập mẫu

> **Lưu ý:** Các tài khoản bên dưới chỉ có sau khi đã import seed data (Bước 4 trong RUN_GUIDE.md gốc).

| Quyền hạn (Role) | Email đăng nhập | Mật khẩu |
|---|---|---|
| 🛡️ **Admin** (Quản trị hệ thống) | `admin@acm.local` | `admin123` |
| 👨‍🌾 **Farmer** (Chủ nông trại) | `farmer@acm.local` | `12345678` |
| 👷 **Employee** (Nhân viên) | `employee@acm.local` | `12345678` |
| 🛒 **Buyer** (Người mua nông sản) | `buyer@acm.local` | `12345678` |

Nếu chưa import seed data, bạn vẫn có thể **đăng ký tài khoản mới** qua giao diện Frontend.

---

## 🌐 9. Danh sách cổng dịch vụ

| Dịch vụ | URL | Port | Mô tả |
|---|---|---|---|
| **Frontend (React)** | http://localhost:5173 | 5173 | Giao diện người dùng |
| **API Gateway** | http://localhost:8000 | 8000 | Điểm vào API tập trung |
| **Identity Service** | http://localhost:8081 | 8081 | Xác thực, JWT, user |
| **Crop Catalog Service** | http://localhost:8082 | 8082 | Danh mục cây trồng |
| **AI Service** | — | 8083 | Chatbot AI Gemini |
| **Farm Service** | http://localhost:8084 | 8084 | Quản lý nông trại |
| **Season Service** | http://localhost:8085 | 8085 | Quản lý mùa vụ |
| **Inventory Service** | http://localhost:8086 | 8086 | Quản lý kho |
| **Finance Service** | http://localhost:8087 | 8087 | Tài chính, chi phí |
| **Incident Service** | http://localhost:8088 | 8088 | Sự cố nông nghiệp |
| **Sustainability Service** | http://localhost:8089 | 8089 | Phát triển bền vững |
| **Marketplace Service** | http://localhost:8090 | 8090 | Sàn giao dịch nông sản |
| **Admin Reporting Service** | http://localhost:8091 | 8091 | Báo cáo quản trị |
| **MySQL** | localhost:3307 | 3307 | Cơ sở dữ liệu |
| **RabbitMQ Console** | http://localhost:15672 | 15672 | `rabbituser` / `rabbitpass` |
| **MinIO Console** | http://localhost:9001 | 9001 | `minioadmin` / `minioadmin` |
| **MailHog** | http://localhost:8025 | 8025 | Hộp thư test (OTP, email) |
| **Grafana** | http://localhost:3001 | 3001 | Dashboard giám sát |
| **Prometheus** | http://localhost:9090 | 9090 | Metrics hệ thống |

---

## 🛠️ 10. Xử lý sự cố (Troubleshooting)

### Service đang `Restarting` liên tục

```bash
# Xem log chi tiết của service bị lỗi
docker logs <tên_container> --tail 100

# Ví dụ:
docker logs identity_service_quanlymuavu --tail 100
```

**Các lỗi thường gặp:**

| Lỗi | Nguyên nhân | Cách sửa |
|---|---|---|
| `Access denied for user 'springuser'@'%'` | MySQL chưa tạo databases hoặc chưa GRANT quyền | `docker compose down -v && docker compose up -d --build` |
| `keys/rsa-private.pem cannot be opened` | Chưa generate RSA keys | Chạy lệnh generate ở mục 3.3 |
| `Schema-validation: missing column` | Flyway migration thiếu column | Kiểm tra và tạo migration file mới |
| `Connection refused` (RabbitMQ/MySQL) | Service phụ thuộc chưa sẵn sàng | Đợi thêm 30s, service sẽ tự retry |

### Reset hoàn toàn và chạy lại từ đầu

```bash
# 1. Dừng tất cả containers VÀ xoá volumes (dữ liệu MySQL, RabbitMQ, MinIO)
docker compose down -v

# 2. Xoá Docker images cũ (nếu muốn rebuild sạch hoàn toàn)
docker compose down -v --rmi local

# 3. Chạy lại
docker compose up -d --build
```

> ⚠️ Lệnh `docker compose down -v` sẽ **xoá toàn bộ dữ liệu**. Script `init-mysql.sql` chỉ chạy lần đầu khi MySQL volume chưa có data.

### Xem log Flyway migration của một service

```bash
docker logs identity_service_quanlymuavu 2>&1 | grep -i "flyway\|migrat"
```

### Kết nối trực tiếp vào MySQL để debug

```bash
# Kết nối với quyền root
docker exec -it mysql_quanlymuavu mysql -uroot -prootpass

# Kết nối với springuser
docker exec -it mysql_quanlymuavu mysql -uspringuser -pspringpass identity_db
```

### Dừng toàn bộ hệ thống (giữ dữ liệu)

```bash
docker compose down
```

### Khởi động lại (không build lại)

```bash
docker compose up -d
```
