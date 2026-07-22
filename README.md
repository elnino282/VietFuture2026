<div align="center">

# Nền tảng Quản lý Mùa vụ cùng Trợ lý ảo — Kiến trúc Microservices

### Agricultural Crop Management (ACM) Platform — Microservices Architecture

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-6DB33F?logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3-61DAFB?logo=react&logoColor=black)](https://reactjs.org/)
[![Vite](https://img.shields.io/badge/Vite-6.3-646CFF?logo=vite&logoColor=white)](https://vitejs.dev/)
[![TailwindCSS](https://img.shields.io/badge/Tailwind%20CSS-4.3-06B6D4?logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Java](https://img.shields.io/badge/Java-23-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-FF6600?logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![Docker](https://img.shields.io/badge/Docker-26-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-Academic-blue.svg)](#license)

**Nền tảng quản lý nông nghiệp toàn diện tích hợp trí tuệ nhân tạo (Google Gemini),
hỗ trợ nông dân tối ưu hóa quy trình canh tác từ gieo trồng đến thu hoạch và thương mại hóa sản phẩm.**
Thiết kế theo kiến trúc **Microservices** với **API Gateway**, **Message Bus (RabbitMQ)**, và **Database-per-Service**.

[Tính năng](#-tính-năng-chính) · [Kiến trúc](#-kiến-trúc-hệ-thống) · [Cài đặt](#-cài-đặt-và-chạy) · [API Docs](#-api-documentation) · [Đóng góp](#-đóng-góp)

</div>

---

## Mục lục

- [Giới thiệu](#-giới-thiệu)
- [Tính năng chính](#-tính-năng-chính)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Danh sách Microservices](#-danh-sách-microservices)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Cấu trúc thư mục](#-cấu-trúc-thư-mục)
- [Cài đặt và chạy](#-cài-đặt-và-chạy)
- [Tài khoản mẫu](#-tài-khoản-mẫu)
- [API Documentation](#-api-documentation)
- [Đa ngôn ngữ](#-đa-ngôn-ngữ-i18n)
- [Triển khai](#-triển-khai)
- [Đóng góp](#-đóng-góp)
- [License](#-license)

---

## Giới thiệu

**ACM Platform** là một nền tảng quản lý nông nghiệp thông minh, được xây dựng nhằm số hóa toàn bộ quy trình quản lý mùa vụ cho nông dân Việt Nam. Hệ thống tích hợp **trợ lý ảo AI** (Google Gemini) giúp tư vấn kỹ thuật canh tác, chẩn đoán bệnh cây trồng, tối ưu hóa chi phí mùa vụ, cùng với một **sàn thương mại nông sản** (Marketplace) để kết nối trực tiếp người nông dân với người mua.

Hệ thống được thiết kế theo kiến trúc **Microservices** với các nguyên tắc:
- **Database-per-Service**: Mỗi service sở hữu database riêng, đảm bảo loose coupling
- **Event-Driven Communication**: Các service giao tiếp qua RabbitMQ message bus
- **Centralized Auth**: JWT authentication tập trung qua Identity Service
- **API Gateway**: Tất cả request từ client đi qua API Gateway

### Mục tiêu

- **Số hóa** quy trình quản lý nông trại, mùa vụ, công việc, thu hoạch
- **Tối ưu hóa** chi phí sản xuất thông qua phân tích AI
- **Minh bạch hóa** nguồn gốc nông sản với hệ thống truy xuất nguồn gốc
- **Kết nối** nông dân và người mua qua sàn thương mại trực tuyến
- **Hỗ trợ** ra quyết định với trợ lý ảo AI và báo cáo phân tích dữ liệu

---

## Tính năng chính

### Trợ lý ảo AI (Google Gemini)

| Tính năng | Mô tả |
|---|---|
| 💬 Chat AI thông minh | Tư vấn kỹ thuật canh tác, trả lời câu hỏi nông nghiệp theo ngữ cảnh |
| 🔬 Chẩn đoán bệnh cây trồng | Nhận diện bệnh qua mô tả/hình ảnh và đề xuất phương pháp xử lý |
| 💰 Tối ưu chi phí mùa vụ | Phân tích và gợi ý cắt giảm chi phí sản xuất dựa trên dữ liệu thực tế |
| 📊 Phân tích dữ liệu | Dashboard thông minh với biểu đồ phân tích dinh dưỡng, đất, nước |

### Quản lý Nông trại (Farmer)

| Tính năng | Mô tả |
|---|---|
| 🏡 Quản lý Farm & Plot | Tạo và quản lý nông trại, thửa đất với thông tin chi tiết |
| 📅 Quản lý Mùa vụ | Lên kế hoạch mùa vụ, theo dõi tiến độ từ gieo trồng đến thu hoạch |
| ✅ Quản lý Công việc | Phân công, theo dõi nhiệm vụ cho nhân viên với deadline và trạng thái |
| 📝 Nhật ký đồng ruộng | Ghi chép hoạt động canh tác hàng ngày (Field Logs) |
| 🌾 Quản lý Thu hoạch | Theo dõi sản lượng, chất lượng thu hoạch theo lô (Product Lots) |
| 📦 Kho vật tư & thành phẩm | Quản lý kho nguyên liệu đầu vào và sản phẩm sau thu hoạch |
| 💸 Quản lý Chi phí | Theo dõi chi phí sản xuất, phân tích lợi nhuận theo mùa vụ |
| ⚠️ Quản lý Sự cố | Ghi nhận và xử lý sự cố phát sinh trong quá trình canh tác |
| 🦠 Theo dõi Dịch bệnh | Giám sát tình hình dịch bệnh cây trồng trên từng thửa đất |
| 👷 Quản lý Nhân công | Quản lý nhân viên, phân công lao động và tính lương |
| 🌤️ Widget Thời tiết | Hiển thị dự báo thời tiết liên quan đến hoạt động nông nghiệp |
| 🔬 Phân tích bền vững | Kiểm tra đất, nước tưới, dinh dưỡng đầu vào cho nông nghiệp bền vững |

### Sàn Thương mại (Marketplace)

| Tính năng | Mô tả |
|---|---|
| 🏪 Catalog sản phẩm | Duyệt, tìm kiếm, lọc sản phẩm nông sản theo danh mục, vùng, giá |
| 🔍 Truy xuất nguồn gốc | Xem toàn bộ hành trình sản phẩm từ nông trại đến tay người mua |
| 🛒 Giỏ hàng & Thanh toán | Thêm sản phẩm, checkout với COD hoặc chuyển khoản ngân hàng |
| 📋 Quản lý Đơn hàng | Theo dõi trạng thái đơn hàng cho cả người mua và người bán |
| ⭐ Đánh giá sản phẩm | Người mua đánh giá và nhận xét sản phẩm sau khi nhận hàng |
| 🏬 Cửa hàng nông trại | Mỗi nông trại có trang riêng hiển thị sản phẩm đang bán |
| 📸 Xác minh thanh toán | Upload bằng chứng chuyển khoản, admin xác minh thanh toán |

### Quản trị hệ thống (Admin)

| Tính năng | Mô tả |
|---|---|
| 👥 Quản lý Người dùng | Quản lý tài khoản, phân quyền (Admin, Farmer, Employee, Buyer) |
| 🌿 Quản lý Danh mục Cây trồng | Quản lý giống cây, loại cây và thông tin kỹ thuật |
| 📊 Báo cáo & Thống kê | Dashboard tổng hợp, báo cáo hoạt động toàn hệ thống |
| 📋 Audit Logs | Theo dõi nhật ký hoạt động người dùng |
| 🔔 Cảnh báo hệ thống | Quản lý thông báo và cảnh báo |
| 📄 Quản lý Tài liệu | Quản lý tài liệu kỹ thuật, hướng dẫn |
| 🛒 Quản lý Marketplace | Duyệt sản phẩm, xác minh thanh toán, quản lý đơn hàng |

### Nhân viên (Employee)

| Tính năng | Mô tả |
|---|---|
| 📋 Xem công việc được giao | Danh sách task từ nông dân với chi tiết và deadline |
| 📈 Cập nhật tiến độ | Báo cáo tiến độ công việc (Progress Logs) |
| 💰 Xem bảng lương | Theo dõi thông tin lương và phụ cấp |

---

## Kiến trúc hệ thống

```
┌────────────────────────────────────────────────────────────────────┐
│                          CLIENT (Browser)                            │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │            React 18 + Vite + TailwindCSS 4                     │ │
│  │  ┌──────────┬──────────┬──────────┬─────────────────────────┐  │ │
│  │  │  Farmer  │  Buyer   │  Admin   │        Employee         │  │ │
│  │  │  Portal  │  Portal  │  Portal  │         Portal          │  │ │
│  │  └──────────┴──────────┴──────────┴─────────────────────────┘  │ │
│  │  ┌──────────┬──────────┬───────────────────────────────────┐  │ │
│  │  │ AI Chat  │ Market   │     Real-time Chat (Firebase)      │  │ │
│  │  └──────────┴──────────┴───────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────┬──────────────────────────────────────┘
                              │ HTTP / WebSocket
                              ▼
┌────────────────────────────────────────────────────────────────────┐
│                         API Gateway (8000)                          │
│  Spring Cloud Gateway — Routing, Auth Header Injection, CORS        │
│  ┌────────────┬────────────┬────────────┬─────────────────────┐   │
│  │ /api/v1/auth│ /api/v1/farm│ /api/v1/season│ /api/v1/marketplace│   │
│  └────────────┴────────────┴────────────┴─────────────────────┘   │
└────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬──────┘
     │    │    │    │    │    │    │    │    │    │    │    │
     ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼
┌────────┐ ┌──────┐ ┌────────┐ ┌────────────┐ ┌──────────────┐
│Identity│ │ Crop │ │   AI   │ │    Farm    │ │    Season    │
│Service │ │Catalog│ │Service │ │  Service   │ │   Service    │
│ (8081) │ │(8082) │ │ (8083) │ │   (8084)   │ │    (8085)    │
└───┬────┘ └──┬───┘ └────┬───┘ └─────┬──────┘ └──────┬───────┘
    │         │          │           │                │
    ▼         ▼          ▼           ▼                ▼
┌────────┐ ┌──────────┐ ┌────────┐ ┌──────────┐ ┌──────────┐
│identity│ │crop_     │ │(Stateless│ │  farm_db  │ │ season_db│
│_db     │ │catalog_db│ │ - No DB)│ │          │ │          │
└────────┘ └──────────┘ └────────┘ └──────────┘ └──────────┘

     ┌───────────────────────────────────────────────────────────┐
     │              RabbitMQ Message Bus (5672)                   │
     │  Topic Exchanges: farm.events, season.events, etc.        │
     └───────────────────────────────────────────────────────────┘
                    │                │                │
                    ▼                ▼                ▼
     ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
     │ Inventory Service │ │  Finance Service │ │  Incident Service │
     │     (8086)       │ │     (8087)       │ │      (8088)       │
     │   inventory_db   │ │    finance_db    │ │    incident_db    │
     └──────────────────┘ └──────────────────┘ └──────────────────┘

     ┌───────────────────────────────────────────────────────────┐
     │        Sustainability Service (8089)  │  Marketplace (8090) │
     │          sustainability_db           │    marketplace_db   │
     └───────────────────────────────────────────────────────────┘

     ┌───────────────────────────────────────────────────────────┐
     │           Admin Reporting Service (8091)                  │
     │              admin_reporting_db                          │
     │   Consumes events → Denormalized read models for admin   │
     └───────────────────────────────────────────────────────────┘
```

---

## Danh sách Microservices

| Service | Port | Database | Mô tả |
|---|---|---|---|
| **api-gateway** | 8000 | — | API Gateway, routing & auth |
| **identity-service** | 8081 | `identity_db` | Auth, users, roles, JWT |
| **crop-catalog-service** | 8082 | `crop_catalog_db` | Crops, varieties, reference data |
| **ai-service** | 8083 | — | Gemini AI integration |
| **farm-service** | 8084 | `farm_db` | Farms, plots |
| **season-service** | 8085 | `season_db` | Seasons, tasks, field logs, harvests |
| **inventory-service** | 8086 | `inventory_db` | Input/output warehouse, stock |
| **finance-service** | 8087 | `finance_db` | Expenses, payroll |
| **incident-service** | 8088 | `incident_db` | Incidents, alerts, notifications |
| **sustainability-service** | 8089 | `sustainability_db` | FDN analysis, soil/water tests |
| **marketplace-service** | 8090 | `marketplace_db` | Products, orders, cart, payments |
| **admin-reporting-service** | 8091 | `admin_reporting_db` | Admin read models, dashboards |

---

## Công nghệ sử dụng

### Backend (Microservices)

| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| **Java** | 23 | Ngôn ngữ lập trình chính |
| **Spring Boot** | 3.5.3 | Framework backend |
| **Spring Cloud Gateway** | — | API Gateway |
| **Spring Security** | — | Xác thực & phân quyền (JWT) |
| **Spring Data JPA** | — | ORM, truy xuất cơ sở dữ liệu |
| **Spring AMQP** | — | RabbitMQ integration |
| **MySQL** | 8.0 | Cơ sở dữ liệu quan hệ (per-service) |
| **Flyway** | — | Database migration |
| **RabbitMQ** | 3.13 | Message bus, event-driven comms |
| **MinIO** | — | Object storage (product images, payment proofs) |
| **Google Gemini SDK** | 1.0.0 | Tích hợp AI |
| **Lombok** | 1.18.30 | Giảm boilerplate code |
| **MapStruct** | 1.5.5 | Object mapping |
| **SpringDoc OpenAPI** | 2.7.0 | Swagger UI |
| **Docker** | — | Container hóa |

### Frontend

| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| **React** | 18.3 | UI library |
| **TypeScript** | 5.6+ | Type-safe JavaScript |
| **Vite** | 6.3.5 | Build tool & dev server |
| **TailwindCSS** | 4.3 | Utility-first CSS framework |
| **TanStack React Query** | 5.x | Server state management |
| **React Router DOM** | 6.26 | Client-side routing |
| **React Hook Form** | 7.55 | Form management + validation |
| **Zod** | 3.23 | Schema validation |
| **Recharts** | 2.15 | Biểu đồ, dashboard |
| **i18next** | 25.8 | Đa ngôn ngữ |
| **Axios** | 1.13 | HTTP client |

---

## Cấu trúc thư mục

```
SE122-Code-MicroserviceReady/
│
├── 📂 api-gateway/                    # API Gateway (Spring Cloud Gateway)
│   ├── src/main/java/
│   └── Dockerfile
│
├── 📂 identity-service/               # Identity & Auth Service
│   ├── src/main/java/org/example/identity/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   ├── src/main/resources/
│   │   └── db/migration/             # Flyway migrations
│   └── Dockerfile
│
├── 📂 crop-catalog-service/           # Crop Catalog Service
│   ├── src/main/java/org/example/cropcatalog/
│   └── Dockerfile
│
├── 📂 ai-service/                     # AI Service (Gemini)
│   ├── src/main/java/org/example/ai/
│   └── Dockerfile
│
├── 📂 farm-service/                   # Farm Management Service
│   ├── src/main/java/org/example/farm/
│   └── Dockerfile
│
├── 📂 season-service/                 # Season Management Service
│   ├── src/main/java/org/example/season/
│   └── Dockerfile
│
├── 📂 inventory-service/              # Inventory Service
│   ├── src/main/java/org/example/inventory/
│   └── Dockerfile
│
├── 📂 finance-service/                # Finance Service
│   ├── src/main/java/org/example/finance/
│   └── Dockerfile
│
├── 📂 incident-service/               # Incident Management Service
│   ├── src/main/java/org/example/incident/
│   └── Dockerfile
│
├── 📂 sustainability-service/          # Sustainability / FDN Service
│   ├── src/main/java/org/example/sustainability/
│   └── Dockerfile
│
├── 📂 marketplace-service/            # Marketplace Service
│   ├── src/main/java/org/example/marketplace/
│   └── Dockerfile
│
├── 📂 admin-reporting-service/        # Admin Reporting Service
│   ├── src/main/java/org/example/adminreporting/
│   └── Dockerfile
│
├── 📂 agricultural-crop-management-frontend/   # Frontend (React + Vite)
│   ├── src/
│   │   ├── features/
│   │   ├── hooks/
│   │   ├── i18n/
│   │   └── services/
│   └── Dockerfile
│
├── 📂 prometheus/                     # Prometheus config
│   └── prometheus.yml
│
├── 📂 docs/                          # Tài liệu dự án
│   ├── API_CONTRACT_MARKETPLACE.md
│   └── firebase/
│
├── 📂 scripts/                        # Scripts hỗ trợ
│
├── docker-compose.yml                 # Full stack (all services + infra)
├── init-mysql.sql                     # Database initialization (infra)
├── prometheus.yml                     # Prometheus config (root level)
└── README.md
```

---

## Cài đặt và chạy

### Yêu cầu hệ thống

| Công cụ | Phiên bản khuyến nghị | Ghi chú |
|---|---|---|
| **Docker** | 20.10+ | Bắt buộc nếu chạy qua Docker Compose |
| **Docker Compose** | 2.0+ | Bắt buộc nếu chạy qua Docker Compose |
| **Java JDK** | 23 | Chỉ cần nếu muốn chạy local bằng IDE hoặc dòng lệnh |
| **Node.js & npm** | 18+ (npm 9+) | Chỉ cần nếu muốn chạy frontend ở chế độ dev local |
| **Git** | 2.30+ | Dùng để clone mã nguồn |

---

### 1️⃣ Chuẩn bị mã nguồn & môi trường

1. **Clone repository về máy**:
   ```bash
   git clone https://github.com/TriThongVoSi/SE122-Code-MicroserviceReady-Test.git
   cd SE122-Code-MicroserviceReady-Test
   ```

2. **Cấu hình file môi trường (`.env`)**:
   Sao chép file `.env.example` thành `.env` ở thư mục gốc:
   * **Windows (Command Prompt)**: `copy .env.example .env`
   * **Windows (PowerShell)**: `Copy-Item .env.example .env`
   * **Linux/macOS**: `cp .env.example .env`

3. **Cấu hình API Key cho Trợ lý ảo AI** (Tùy chọn nhưng cần thiết cho Chatbot):
   Mở file `.env` vừa tạo và điền khóa API Gemini của bạn vào biến:
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key
   ```
   > [!NOTE]
   > Hệ thống sử dụng cơ chế xác thực bất đối xứng **RS256 JWKS** (không sử dụng secret key đối xứng `JWT_SIGNER_KEY` cũ nữa). Cặp khóa RSA (private & public key) đã được tích hợp sẵn ở `identity-service` trong thư mục `src/main/resources/keys/` và tự động load khi khởi động.

---

### 2️⃣ Khởi chạy toàn bộ hệ thống bằng Docker Compose (Khuyến nghị)

Đây là cách nhanh nhất và ổn định nhất để chạy đầy đủ ứng dụng với tất cả 12 microservices, cơ sở dữ liệu tách biệt và hạ tầng hỗ trợ.

1. **Chạy lệnh khởi động**:
   ```bash
   docker-compose up -d --build
   ```
   *Lệnh này sẽ tải các Docker images cần thiết, build tất cả microservices từ mã nguồn và khởi chạy chúng ngầm.*

2. **Kiểm tra trạng thái các container**:
   Đợi khoảng 60 - 90 giây để các cơ sở dữ liệu khởi tạo và chạy Flyway migrations, sau đó gõ lệnh:
   ```bash
   docker-compose ps
   ```
   *Đảm bảo tất cả các container đều hiển thị trạng thái `Up` (hoặc `healthy`).*

3. **Truy cập ứng dụng**:
   * **Frontend**: `http://localhost:3000` (đăng nhập bằng tài khoản mẫu bên dưới)
   * **Hạ tầng quản lý RabbitMQ**: `http://localhost:15672` (User: `rabbituser` | Pass: `rabbitpass`)
   * **Hạ tầng quản lý MinIO**: `http://localhost:9001` (User: `minioadmin` | Pass: `minioadmin`)
   * **MailHog Web**: `http://localhost:8025` (Kiểm tra hộp thư thử nghiệm gửi từ hệ thống)
   * **Grafana Dashboard**: `http://localhost:3001` (Thông tin giám sát hệ thống)

---

### 3️⃣ Chạy ở chế độ Phát triển (Local Development)

Nếu bạn muốn chỉnh sửa mã nguồn và debug trực tiếp bằng IDE (như IntelliJ IDEA, VS Code):

#### Bước 1: Chỉ chạy hạ tầng phụ trợ trong Docker
Khởi chạy cơ sở dữ liệu, message broker và lưu trữ để các microservices local kết nối đến:
```bash
docker-compose up -d mysql rabbitmq minio mailhog
```

#### Bước 2: Chạy các dịch vụ Backend mong muốn qua dòng lệnh hoặc IDE
Đảm bảo bạn đã cài đặt JDK 23 và biến môi trường `JAVA_HOME` đã trỏ đúng phiên bản.
Di chuyển vào thư mục của service cụ thể (ví dụ `identity-service` hoặc `farm-service`) và chạy lệnh:
* **Windows**:
  ```cmd
  mvnw spring-boot:run -Dspring-boot.run.profiles=dev
  ```
* **Linux/macOS**:
  ```bash
  ./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
  ```
*(Hoặc click nút Run/Debug nút mũi tên xanh trên IDE đối với class Application chính của service đó).*

#### Bước 3: Chạy ứng dụng Frontend ở chế độ Dev
Mở một terminal riêng biệt tại thư mục frontend, cài đặt dependencies và chạy dev server:
```bash
cd agricultural-crop-management-frontend
npm install
npm run dev
```
Trình duyệt sẽ tự động mở trang web giao diện tại địa chỉ: `http://localhost:5173`.

---

### 4️⃣ Hướng dẫn Import dữ liệu mẫu vào Database (Seed Data)

Khi cơ sở dữ liệu được tạo mới lần đầu tiên, nó sẽ trống. Để nhập dữ liệu nông trại mẫu, sản phẩm sàn thương mại, danh mục cây trồng, bạn cần chạy script import dữ liệu.

Vui lòng tham khảo tài liệu hướng dẫn cụ thể theo hệ điều hành của bạn:
* 🪟 **Trên Windows**: [Hướng dẫn luồng thao tác import dữ liệu mới window.md](Hướng%20dẫn%20luồng%20thao%20tác%20import%20dữ%20liệu%20mới%20window.md)
* 🐧 **Trên Linux**: [Hướng dẫn luồng thao tác import dữ liệu mới linux.md](Hướng%20dẫn%20luồng%20thao%20tác%20import%20dữ%20liệu%20mới%20linux.md)

---

### 5️⃣ Kiểm tra & Giám sát dịch vụ (Health Check)

Bạn có thể giám sát trạng thái hoạt động của các dịch vụ bằng lệnh curl hoặc trình duyệt:
* **Kiểm tra API Gateway**: [http://localhost:8000/actuator/health](http://localhost:8000/actuator/health)
* **Kiểm tra Identity Service**: [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)
* **Đường dẫn Prometheus Metrics**: [http://localhost:9090](http://localhost:9090)

---

---

## Tài khoản mẫu

Mỗi service có Flyway migrations chạy tự động khi khởi tạo. Identity Service tạo sẵn các tài khoản mẫu sau khi schema được apply:

### Tài khoản chính

| Role | Email | Mật khẩu |
|---|---|---|
| 🛡️ **Admin** | `admin@acm.local` | `admin123` |
| 👨‍🌾 **Farmer** | `farmer@acm.local` | `12345678` |
| 👷 **Employee** | `employee@acm.local` | `12345678` |
| 🛒 **Buyer** | `buyer@acm.local` | `12345678` |

---

## API Documentation

### Swagger UI

Mỗi service có Swagger UI riêng:

| Service | Swagger URL |
|---|---|
| API Gateway | `http://localhost:8000/swagger-ui.html` |
| Identity Service | `http://localhost:8081/swagger-ui.html` |
| Farm Service | `http://localhost:8084/swagger-ui.html` |
| Season Service | `http://localhost:8085/swagger-ui.html` |
| Marketplace Service | `http://localhost:8090/swagger-ui.html` |

### API Endpoint tổng quan

| Prefix | Mô tả | Auth |
|---|---|---|
| `GET /api/v1/marketplace/products/**` | Catalog sản phẩm công khai | Public |
| `GET /api/v1/marketplace/farms/**` | Thông tin nông trại công khai | Public |
| `GET /api/v1/marketplace/traceability/**` | Truy xuất nguồn gốc | Public |
| `/api/v1/auth/**` | Authentication | Public |
| `/api/v1/farm/**` | Farm Management | FARMER |
| `/api/v1/season/**` | Season Management | FARMER, EMPLOYEE |
| `/api/v1/marketplace/cart/**` | Giỏ hàng | BUYER |
| `/api/v1/marketplace/orders/**` | Đơn hàng | BUYER |
| `/api/v1/marketplace/farmer/**` | Marketplace - Seller | FARMER |
| `/api/v1/admin/**` | Admin API | ADMIN |
| `/api/v1/marketplace/admin/**` | Marketplace Admin | ADMIN |

> 📖 Chi tiết API contract cho Marketplace xem tại [`docs/API_CONTRACT_MARKETPLACE.md`](docs/API_CONTRACT_MARKETPLACE.md)
> ⚠️ **Lưu ý:** Các tài liệu OpenAPI (`acm-monolith-v1.*`) cũ từ kiến trúc nguyên khối (monolith) đã được lưu trữ vào thư mục [`docs/audit-history/legacy-monolith-spec/`](docs/audit-history/legacy-monolith-spec/) để phục vụ mục đích tham khảo lịch sử. Chúng không phản ánh kiến trúc microservices hiện tại.

---

## Đa ngôn ngữ (i18n)

Ứng dụng hỗ trợ đa ngôn ngữ thông qua **i18next**:

- 🇻🇳 **Tiếng Việt** (mặc định)
- 🇬🇧 **English**

Người dùng có thể chuyển đổi ngôn ngữ trực tiếp trên giao diện. Hệ thống tự động phát hiện ngôn ngữ trình duyệt.

---

## Triển khai

### Docker Compose (Production)

```bash
# Build all images
docker-compose build

# Run all services
docker-compose up -d

# Scale a specific service
docker-compose up -d --scale farm-service=2
```

### Monitoring

```bash
# Prometheus metrics
open http://localhost:9090

# Grafana dashboards
open http://localhost:3001  # admin / admin
```

---

## Đóng góp

1. **Fork** repository
2. Tạo **feature branch**: `git checkout -b feature/ten-tinh-nang`
3. **Commit** thay đổi: `git commit -m "feat: mô tả tính năng"`
4. **Push** lên branch: `git push origin feature/ten-tinh-nang`
5. Tạo **Pull Request**

### Quy tắc commit message

```
feat:     Tính năng mới
fix:      Sửa lỗi
docs:     Thay đổi tài liệu
style:    Format code (không thay đổi logic)
refactor: Tái cấu trúc code
test:     Thêm/sửa test
chore:    Cập nhật build, config
```

---

## License

Dự án này được phát triển phục vụ mục đích học tập trong khuôn khổ môn học **SE122 — Đồ án 2** tại trường Đại học Công nghệ Thông tin (UIT).

---

<div align="center">

**Được xây dựng với ❤️ bởi Võ Sĩ Trí Thông & Hồ Ngọc Quỳnh**

🌾 *Nền tảng Quản lý Mùa vụ cùng Trợ lý ảo — Số hóa nông nghiệp Việt Nam* 🌾

</div>
