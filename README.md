# DMG — E-commerce Order Management System

Spring Boot REST API for multi-warehouse e-commerce order management at scale.

**Repository:** https://github.com/champhanu/DMG  
**Stack:** Java 21 · Spring Boot 4.1 · MySQL (local) · H2 (tests)

---

## Problem Statement

Build an order management system with:

- Multi-category product catalog
- Customer cart and checkout
- Inventory across multiple warehouses with **concurrent-safe reservation** (no overselling)
- Payment processing
- Order lifecycle: `PLACED → CONFIRMED → PACKED → SHIPPED → DELIVERED → RETURNED`
- Atomic order placement (cart + inventory + payment in one transaction)
- Non-blocking downstream pipeline (fulfillment routing, notifications, audit logging)
- Discounts, taxes, returns, and refunds
- Role-based access: **Admin**, **Customer**, **Warehouse Staff**

---

## Assumptions & Scoping Decisions

> Updated incrementally as each module is built. Every decision is documented here.

| # | Assumption | Reasoning | Status |
|---|------------|-----------|--------|
| 1 | Monolithic Spring Boot app (no microservices) | Per assignment out-of-scope | ✅ Locked |
| 2 | No UI / frontend | Per assignment out-of-scope | ✅ Locked |
| 3 | Basic RBAC via Spring Security (no OAuth/SSO/MFA) | Per assignment scope | 🔲 Step 3 |
| 4 | MySQL locally, Hibernate `ddl-auto: update` | Tables auto-created/updated on app start; no Flyway yet | ✅ Step 1 |
| 5 | Async pipeline via Spring `@Async` + application events | Avoids blocking checkout without distributed infra | 🔲 Step 8 |
| 6 | Inventory reservation uses pessimistic locking / DB constraints | Prevents overselling under concurrency | 🔲 Step 5 |
| 7 | Hierarchical categories (parent/child) | Supports multi-category catalog browsing | ✅ Step 2 |
| 8 | Soft delete for catalog (`active=false`) | Preserves order history integrity for future modules | ✅ Step 2 |
| 9 | RBAC enforced on catalog APIs | Deferred to Step 3; endpoints documented by intended role | 🔲 Step 3 |

---

## Module Roadmap

Development proceeds **one module at a time**. Each step = one focused commit + README/AGENTS update.

| Step | Module | Description | Status |
|------|--------|-------------|--------|
| 0 | **Skeleton** | README, AGENTS.md, package layout, skills docs | ✅ Done |
| 1 | **Project foundation** | JPA, Security, Validation, MySQL config, health endpoint | ✅ Done |
| 2 | **Catalog management** | Categories, products, admin CRUD, customer browse APIs | ✅ Done |
| 3 | **Auth & RBAC** | Users, roles (ADMIN, CUSTOMER, WAREHOUSE_STAFF), secure APIs | 🔲 Pending |
| 4 | **Inventory** | Warehouses, stock levels, concurrent reservation | 🔲 Pending |
| 5 | **Cart** | Add/update/remove items, cart persistence | 🔲 Pending |
| 6 | **Discounts & Tax** | Promo codes, order-level tax calculation | 🔲 Pending |
| 7 | **Checkout & Orders** | Atomic placement, payment stub, order state machine | 🔲 Pending |
| 8 | **Fulfillment** | Warehouse routing, staff status updates | 🔲 Pending |
| 9 | **Returns & Refunds** | Return requests, refund processing | 🔲 Pending |
| 10 | **Async pipeline** | Events: notifications, audit log, fulfillment routing | 🔲 Pending |
| 11 | **Tests** | Unit + integration tests for core flows | 🔲 Ongoing |

---

## Planned Package Structure

```
src/main/java/Ecommerce/Management/
├── config/          # Security, async, DB, OpenAPI
├── controller/      # REST endpoints
├── domain/          # JPA entities & enums
├── dto/             # Request/response objects
├── event/           # Domain events & async listeners
├── exception/       # Global error handling
├── repository/      # Spring Data repositories
├── security/        # RBAC, auth filters
└── service/         # Business logic
```

---

## Catalog API

Base path: `http://localhost:8080`

### Categories

| Method | Endpoint | Role (intended) | Description |
|--------|----------|-----------------|-------------|
| `GET` | `/api/categories` | Customer / Admin | List root categories (`?parentId=` for children) |
| `GET` | `/api/categories/{id}` | Customer / Admin | Get category by id |
| `POST` | `/api/categories` | Admin | Create category |
| `PUT` | `/api/categories/{id}` | Admin | Update category |
| `DELETE` | `/api/categories/{id}` | Admin | Soft-deactivate category |

Query params: `parentId`, `includeInactive` (admin, default `false`)

### Products

| Method | Endpoint | Role (intended) | Description |
|--------|----------|-----------------|-------------|
| `GET` | `/api/products` | Customer / Admin | List products (paginated) |
| `GET` | `/api/products/{id}` | Customer / Admin | Get product by id |
| `POST` | `/api/products` | Admin | Create product |
| `PUT` | `/api/products/{id}` | Admin | Update product |
| `DELETE` | `/api/products/{id}` | Admin | Soft-deactivate product |

Query params: `categoryId`, `search`, `includeInactive`, `page`, `size`, `sort`

### Example: create a category

```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -d '{"name":"Electronics","slug":"electronics","description":"Devices"}'
```

### Example: create a product

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"DMG Phone X","sku":"PHONE-X-001","description":"Flagship","price":699.99,"categoryId":1}'
```

MySQL tables `categories` and `products` are created automatically on first app start.

---

## API Surface (planned — remaining modules)

| Area | Endpoints (draft) | Role |
|------|-------------------|------|
| Inventory | `GET/POST /api/warehouses`, `/api/inventory` | Admin |
| Cart | `GET/POST/PUT/DELETE /api/cart` | Customer |
| Checkout | `POST /api/checkout` | Customer |
| Orders | `GET /api/orders`, `POST /api/orders/{id}/return` | Customer |
| Fulfillment | `PATCH /api/orders/{id}/status` | Warehouse Staff |
| Discounts | `POST/GET /api/discounts` | Admin |

---

## How to Run

### 1. Start MySQL and create the database (one time)

```bash
mysql -u root -p < sql/init.sql
```

Or let Spring auto-create it via the JDBC URL (`createDatabaseIfNotExist=true`).

### 2. Set your MySQL password locally

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# Edit application-local.yml with your MySQL root password
```

Or use environment variables:

```bash
export DMG_DB_USERNAME=root
export DMG_DB_PASSWORD=your-password
```

### 3. Start the app

```bash
./mvnw spring-boot:run
```

Health check: http://localhost:8080/actuator/health

Hibernate `ddl-auto: update` will create and update tables automatically as entities are added.

---

## How to Test

```bash
./mvnw test
```

---

## Development Log

| Date | Step | Commit message |
|------|------|----------------|
| 2026-06-22 | 0 — Skeleton | `docs: project skeleton with README, AGENTS.md, and module roadmap` |
| 2026-06-22 | 1 — Foundation | `feat: project foundation with MySQL, JPA, security, and health endpoint` |
| 2026-06-22 | 2 — Catalog | `feat: catalog management with categories, products, and REST APIs` |

---

## Out of Scope (per assignment)

- UI / frontend
- Deployment, Docker, CI/CD
- Microservices / distributed systems
- OAuth, SSO, MFA
- Production observability / monitoring
