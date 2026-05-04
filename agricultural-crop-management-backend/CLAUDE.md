# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.3 backend for an agricultural crop management platform (QuanLyMuaVu). The system manages farms, seasons, crops, inventory, marketplace, sustainability metrics, and includes AI-powered features via Google Gemini and Firebase chat integration.

**Tech Stack:**
- Java 23
- Spring Boot 3.5.3 with Spring Security (OAuth2 Resource Server)
- MySQL 8.0 with Flyway migrations
- JWT authentication (custom implementation with nimbus-jose-jwt)
- Lombok + MapStruct for code generation
- Google Gemini AI integration
- Firebase Admin SDK for chat features
- SpringDoc OpenAPI for API documentation

## Development Commands

### Build and Run

```bash
# Build the project
./mvnw clean install

# Run the application (dev profile by default)
./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

# Build without tests
./mvnw clean install -DskipTests
```

### Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=AuthenticationControllerTest

# Run specific test method
./mvnw test -Dtest=AuthenticationControllerTest#testLogin

# Run tests with coverage
./mvnw clean test jacoco:report
```

### Database

```bash
# Start MySQL and MailHog via Docker Compose
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f mysql
```

**Flyway Migrations:**
- Migrations are in `src/main/resources/db/migration/`
- Flyway is **disabled by default** (`spring.flyway.enabled=false`)
- Enable with `SPRING_FLYWAY_ENABLED=true` environment variable
- Baseline version is V13 (configurable via `SPRING_FLYWAY_BASELINE_VERSION`)
- 19 migrations currently exist (V10-V20, plus B19 baseline)

### API Documentation

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### MailHog (Development Email Testing)

- Web UI: http://localhost:8025
- SMTP: localhost:1025

## Architecture

### Module Structure

The codebase follows a **modular monolith** architecture with domain-driven design principles. Each module is self-contained under `src/main/java/org/example/QuanLyMuaVu/module/`:

**Core Modules:**
- `identity` - User authentication, authorization, roles, JWT management
- `farm` - Farm management, field/plot tracking
- `season` - Growing season lifecycle management
- `cropcatalog` - Crop type definitions and catalog
- `inventory` - Input/output inventory tracking
- `financial` - Financial transactions and records
- `incident` - Incident reporting and tracking
- `sustainability` - Sustainability metrics, nitrogen use efficiency (NUE), alerts
- `marketplace` - Product listings, orders, payments, farmer/buyer interactions
- `ai` - Google Gemini AI integration for agricultural advice
- `shared` - Cross-cutting concerns (security config, common patterns)
- `admin` - Administrative dashboards and operations

**Standard Module Layout:**
Each module typically contains:
- `controller/` - REST API endpoints
- `service/` - Business logic
- `repository/` - JPA repositories
- `entity/` - JPA entities
- `dto/` - Request/response DTOs
- `mapper/` - MapStruct mappers
- `port/` - Port interfaces (hexagonal architecture pattern)
- `config/` - Module-specific configuration

### Security Architecture

**Authentication Flow:**
1. JWT-based authentication using custom `CustomJwtDecoder` (in `identity` module)
2. OAuth2 Resource Server configuration in `shared/config/SecurityConfig.java`
3. Stateless sessions (no server-side session storage)
4. Role-based access control with predefined roles: ADMIN, FARMER, EMPLOYEE, BUYER

**Public Endpoints:**
- `/api/v1/auth/**` - Authentication endpoints (sign-in, sign-up, password reset)
- `/api/v1/public/**` - Public API
- `/api/v1/address/**` (GET) - Address lookup for dropdowns
- `/api/v1/marketplace/products/**` (GET) - Public marketplace catalog
- Swagger/OpenAPI endpoints

**Default Test Accounts** (created by `ApplicationInitConfig`):
- Admin: `admin` / `admin123`
- Farmer: `farmer` / `12345678`
- Employee: `employee` / `12345678`
- Buyer: `buyer` / `12345678`

### Configuration Management

**Environment Variables** (see `.env.example`):
- `SERVER_PORT` - Server port (default: 8080)
- `DB_URL`, `DB_USER`, `DB_PASS` - MySQL connection
- `JWT_SIGNER_KEY` - JWT signing key (required)
- `JWT_VALID_DURATION` - Token validity in seconds (default: 3600)
- `JWT_REFRESHABLE_DURATION` - Refresh token duration (default: 36000)
- `FIREBASE_PROJECT_ID`, `FIREBASE_SERVICE_ACCOUNT_JSON` - Firebase config (optional)
- `APP_AI_API_KEY` - Google Gemini API key
- `MAIL_ENABLED`, `SMTP_HOST`, `SMTP_PORT` - Email configuration

**Profiles:**
- `dev` (default) - Development with MailHog, debug logging
- `prod` - Production configuration
- `module-smoke-*` - Module isolation testing profiles

**Application Properties:**
- Main config: `src/main/resources/application.properties`
- Profile-specific: `application-{profile}.yml`
- Structured properties: `AppProperties.java` with nested classes for JWT, Mail, AI, Marketplace, Sustainability

### Sustainability Module

The sustainability module calculates nitrogen use efficiency (NUE) and provides alerts:

**Key Configuration** (in `application.yml`):
- `app.sustainability.threshold-source` - Alert threshold source
- `app.sustainability.alerts.*` - Alert level thresholds (low/medium/high)
- `app.sustainability.score-weights.*` - Scoring weights (dependency, efficiency, productivity, risk, confidence)
- `app.sustainability.estimation.*` - NUE estimation parameters (atmospheric deposition, irrigation, legume fixation, fertilizer N ratios)

**Legacy Backfill:**
- Disabled by default (`app.sustainability.legacy-backfill.enabled=false`)
- Dry-run mode available for testing

### Testing Strategy

**Test Types:**
- Unit tests: `src/test/java/.../Service/`
- Integration tests: `src/test/java/.../controller/*IntegrationTest.java`
- Architecture tests: `src/test/java/.../Architecture/` (using ArchUnit)
  - `ArchitectureBaselineTest` - Enforces architectural boundaries
  - `PriorityModuleBoundaryTest` - Module dependency rules

**Test Configuration:**
- H2 in-memory database for tests
- Mockito with dynamic agent loading enabled (`-XX:+EnableDynamicAgentLoading`)
- Spring Security Test support

## Key Patterns and Conventions

### Annotation Processing

The project uses Lombok and MapStruct with proper annotation processor ordering:
1. Lombok processes first
2. lombok-mapstruct-binding bridges the two
3. MapStruct processes last

This order is critical for MapStruct to see Lombok-generated getters/setters.

### API Response Format

Standard response wrapper: `ApiResponse<T>` in `DTO/Common/`
- Paginated responses: `PageResponse<T>`

### Error Handling

Custom exceptions in `Exception/` package with global exception handler.

### Logging

- DEBUG level enabled for application code and Spring Security
- Structured console logging pattern: `%d{yyyy-MM-dd HH:mm:ss} - %msg%n`

## Common Development Tasks

### Adding a New Module

1. Create module package under `src/main/java/org/example/QuanLyMuaVu/module/{module-name}/`
2. Follow standard module layout (controller, service, repository, entity, dto, mapper)
3. Add module-specific configuration if needed
4. Update `SecurityConfig` if new endpoints require special authorization
5. Create Flyway migration for new tables
6. Add architecture tests to enforce module boundaries

### Adding a New API Endpoint

1. Create DTO classes in module's `dto/` package
2. Add controller method with proper Spring Security annotations (`@PreAuthorize`)
3. Implement service layer logic
4. Add OpenAPI annotations for Swagger documentation
5. Write unit and integration tests
6. Update `SecurityConfig` if endpoint should be public

### Database Schema Changes

1. Create new Flyway migration: `V{next-number}__{description}.sql`
2. Test migration with `SPRING_FLYWAY_ENABLED=true`
3. Update JPA entities to match schema
4. Regenerate MapStruct mappers if needed (`./mvnw clean compile`)

### Working with JWT

- JWT configuration in `application.yml` under `jwt:` key
- Custom decoder: `CustomJwtDecoder` in `identity/config/`
- Token introspection endpoint: `/api/v1/auth/introspect`
- Refresh token endpoint: `/api/v1/auth/refresh`

### AI Integration

- Google Gemini client configured in `ai` module
- Base URL: `https://generativelanguage.googleapis.com`
- Model: `gemini-2.5-flash` (configurable via `APP_AI_MODEL`)
- API key required in environment

## Important Notes

- **Java 23 Required**: Project uses Java 23 features and Spring Boot 3.x compatibility
- **Flyway Disabled by Default**: Enable explicitly to avoid unexpected schema changes
- **CORS Configured**: Check `CorsConfig.java` for allowed origins
- **Proxy Headers**: Disabled by default (`farm.security.trust-proxy-headers=false`)
- **Password Encoding**: BCrypt via Spring Security
- **Session Management**: Stateless (no server-side sessions)
- **Character Encoding**: UTF-8 enforced for database connections
