# Kế hoạch chuyển ACM từ Modular Monolith sang Microservices

## Tóm Tắt

- Chỉ microservice hóa backend trong giai đoạn này. Frontend React giữ là một SPA/container riêng, chạy sau API Gateway.
- Docker dùng để đóng gói từng service; Docker Compose dùng local/staging. Đích production nên dùng Kubernetes hoặc hạ tầng orchestration tương đương.
- Đích cuối là production-grade: service sở hữu dữ liệu riêng, giao tiếp qua REST + event, không đọc bảng của nhau. Giai đoạn đầu dùng schema riêng trong cùng MySQL để giảm rủi ro, sau đó tách DB vật lý.

## Kiến Trúc Đích

- `frontend`: React/Vite build thành static site, served bằng Nginx, gọi một API base duy nhất qua Gateway.
- `api-gateway`: route `/api/v1/**`, xác thực JWT, CORS, rate limit, correlation id, reverse proxy đến service nội bộ.
- Backend services:
  - `identity-service`: auth, users, roles, preferences, password reset, Google login, JWT/JWKS.
  - `farm-service`: farms, plots, provinces/wards/address lookup.
  - `crop-catalog-service`: crops, varieties, crop nitrogen references.
  - `season-service`: seasons, tasks, field logs, harvests, disease records, labor/payroll.
  - `inventory-service`: supplies, warehouses, stock movements, product warehouse lots.
  - `finance-service`: expenses, season cost data.
  - `incident-service`: incidents, alerts, notifications.
  - `sustainability-service`: soil tests, nutrient inputs, irrigation analyses, sustainability metrics, weather.
  - `ai-service`: Gemini chat, disease suggestion, cost optimization, image/search analysis APIs.
  - `marketplace-service`: products, cart, orders, buyer addresses, reviews, payment proofs, order lifecycle.
  - `admin-reporting-service`: admin dashboard, audit logs, documents, reporting/read models.
- Platform components:
  - MySQL schema-per-service initially, later DB-per-service.
  - RabbitMQ for domain events.
  - MinIO/S3-compatible storage for marketplace images/payment proofs.
  - OpenTelemetry + Prometheus/Grafana + centralized logs.

## Key Changes

- Keep public API path stable for frontend: `/api/v1/...`; Gateway maps existing routes to new services.
- Move frontend env from backend direct URL to gateway URL, for example `VITE_API_BASE_URL=/api` or gateway origin.
- Replace local Spring `ApplicationEventPublisher` cross-module events with RabbitMQ events using outbox pattern.
- Remove JPA cross-module entity relationships from extracted services; use IDs and immutable snapshots instead.
- Each service owns:
  - its own Maven/Spring Boot app,
  - its own Dockerfile,
  - its own Flyway migrations,
  - its own schema user/password,
  - its own OpenAPI contract.
- Shared code is limited to DTO/event envelopes, observability, error shape, and security helpers. Do not share JPA entities/repositories.

## Migration Phases
 
1. Foundation
   - Add Gateway, RabbitMQ, MinIO, observability, service template, CI build matrix.
   - Freeze public API contracts from current controllers.
   - Add ArchUnit rules: no cross-module entity/repository/service dependency except approved ports.

2. Low-Risk Extractions
   - Extract `identity-service`, `crop-catalog-service`, and `ai-service` first.
   - Gateway keeps old route names, so frontend changes are minimal.
   - Convert JWT to asymmetric signing/JWKS so every service can validate tokens independently.

3. Core Agriculture Split
   - Extract `farm-service`, then `season-service`, then `inventory-service`.
   - Publish events: `FarmUpdated`, `SeasonCreated`, `TaskAssigned`, `TaskCompleted`, `HarvestRecorded`, `StockAdjusted`.
   - Inventory consumes harvest events and owns warehouse/product-lot state.

4. Business Workflows
   - Extract `finance-service`, `incident-service`, and `sustainability-service`.
   - Incident/notification consumes task, expense, harvest, and incident events.
   - Sustainability consumes farm/season/crop snapshots instead of joining source tables.

5. Marketplace
   - Extract `marketplace-service`.
   - Monolith marketplace routes are disabled and deprecated; keep the old module only as an internal rollback/fallback reference.
   - Checkout calls inventory synchronously to reserve stock using `X-Idempotency-Key`.
   - Orders publish `OrderCreated`, `PaymentSubmitted`, `PaymentVerified`, `OrderCompleted`, `OrderCancelled`.
   - Traceability uses marketplace-owned snapshot projections copied from inventory/farm/season APIs; Gateway/BFF aggregation is not used for this flow.

6. Admin And Reporting
   - Convert current admin dashboard/report APIs into `admin-reporting-service`.
   - Build read models from service events.
   - Remove all admin direct reads across domain schemas before final DB split.

7. Final Hardening
   - Move from schema-per-service to DB-per-service.
   - Remove cross-schema foreign keys.
   - Enable backups, health checks, readiness/liveness probes, dashboards, alerting, rollback playbooks.

## Test Plan

- Backend: keep current unit/integration tests, then move them beside each extracted service.
- Contract tests: OpenAPI compatibility for Gateway routes and frontend clients.
- Service tests: Testcontainers for MySQL, RabbitMQ, MinIO.
- Migration tests: verify data moved to service schemas, no missing FK-equivalent references, idempotent outbox delivery.
- E2E smoke flows:
  - Admin manages users, reports, marketplace approval.
  - Farmer manages farm, plot, season, task, expense, harvest, inventory.
  - Buyer browses marketplace, cart, checkout, payment proof, review, traceability.
  - Employee sees assigned task, progress log, payroll.
- Non-functional checks: auth propagation, CORS, rate limit, retry/idempotency, event replay, service downtime fallback.

## Assumptions And Defaults

- Frontend will not be split into micro-frontends now.
- Current repo remains a monorepo during migration; services can move to separate repos later.
- Docker Compose is for local/staging only; production target is orchestrated deployment.
- Database strategy is phased: schema-per-service first, DB-per-service as final production state.
- RabbitMQ is selected because current domain events are business events, not high-volume analytics streams.
