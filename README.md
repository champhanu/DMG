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
- Order lifecycle: `CREATED → CONFIRMED → PACKED → SHIPPED → DELIVERED` with `RETURNED` (from delivered) and `CANCELLED` (from created or confirmed only)
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
| 3 | Basic RBAC via Spring Security HTTP Basic (no OAuth/SSO/MFA) | `ADMIN`, `CUSTOMER`, `WAREHOUSE_STAFF` roles; toggle via `dmg.security.enforce-rbac` | ✅ Done |
| 4 | MySQL locally, Hibernate `ddl-auto: update` | Tables auto-created/updated on app start; no Flyway yet | ✅ Step 1 |
| 5 | Async pipeline via Spring `@Async` + application events | Checkout publishes events after commit | ✅ Checkout |
| 6 | Inventory reservation uses pessimistic locking | `PESSIMISTIC_WRITE` on stock rows during checkout | ✅ Checkout |
| 7 | Hierarchical categories (parent/child) | Supports multi-category catalog browsing | ✅ Step 2 |
| 8 | Soft delete for catalog (`active=false`) | Preserves order history integrity for future modules | ✅ Step 2 |
| 9 | RBAC enforced on APIs | Role-based URL rules in `SecurityConfig` | ✅ Done |
| 10 | Cart keyed by `customerId` until Auth | Placeholder until User entity + security principal in Step 3 | ✅ Cart step |
| 11 | Unit price snapshotted on add/update | Keeps cart total stable if catalog price changes before checkout | ✅ Cart step |
| 12 | Configurable tax via `TaxService` | `dmg.tax.rate` in `application.yml` (default 10%) | ✅ Done |
| 13 | Promo codes at checkout | `DiscountService` + `promoCode` on checkout request | ✅ Done |
| 14 | Payment gateway stubbed via `PaymentGateway` interface | `charge()` + `refund()`; swappable for real PSP | ✅ Payment |
| 15 | Refunds support partial and full | `PARTIALLY_REFUNDED` / `REFUNDED`; cancel/return auto-refund | ✅ Payment |
| 16 | Order state machine + optimistic locking | `@Version` on `orders`; cancel only before `PACKED` | ✅ Orders |
| 17 | Async fulfillment routing + audit persistence | `@Async` listeners after commit; `fulfillment_tasks` + `audit_logs` tables | ✅ Done |

---

## Module Roadmap

Development proceeds **one module at a time**. Each step = one focused commit + README/AGENTS update.

| Step | Module | Description | Status |
|------|--------|-------------|--------|
| 0 | **Skeleton** | README, AGENTS.md, package layout, skills docs | ✅ Done |
| 1 | **Project foundation** | JPA, Security, Validation, MySQL config, health endpoint | ✅ Done |
| 2 | **Catalog management** | Categories, products, admin CRUD, customer browse APIs | ✅ Done |
| 3 | **Auth & RBAC** | Users, roles, HTTP Basic security | ✅ Done |
| 4 | **Inventory & Warehouses** | Multi-warehouse stock, admin APIs, concurrent reservation | ✅ Done |
| 5 | **Cart** | Add/update/remove items, cart persistence | ✅ Done |
| 6 | **Checkout & Orders** | Atomic placement, payment, order APIs | ✅ Done |
| 7 | **Payments** | Gateway, payment APIs, refunds | ✅ Done |
| 8 | **Order management** | State machine, cancel, return, status APIs | ✅ Done |
| 9 | **Discounts & Tax** | Promo codes, configurable tax rate | ✅ Done |
| 10 | **Fulfillment** | Warehouse routing tasks, staff APIs | ✅ Done |
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

## Cart API

Base path: `http://localhost:8080`

| Method | Endpoint | Role (intended) | Description |
|--------|----------|-----------------|-------------|
| `GET` | `/api/cart?customerId={id}` | Customer | Get active cart (404 if none) |
| `POST` | `/api/cart/items` | Customer | Add one product with quantity |
| `POST` | `/api/cart/items/bulk` | Customer | Add multiple products with quantities in one request |
| `PUT` | `/api/cart/items/{itemId}` | Customer | Update item quantity |
| `DELETE` | `/api/cart/items/{itemId}` | Customer | Remove line item |
| `DELETE` | `/api/cart?customerId={id}` | Customer | Clear all items |

`customerId` is a placeholder until Auth is implemented (Step 3).

### Example: add to cart

```bash
curl -X POST http://localhost:8080/api/cart/items \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"productId":1,"quantity":2}'
```

Response includes `items` (one line per product), `lineCount` (distinct products), `totalItems` (sum of quantities), and `subtotal`. Each line includes `categoryId` and `categoryName` so items from different catalog categories are visible. Adding the same product again merges quantities.

### Example: add multiple catalog products at once

```bash
curl -X POST http://localhost:8080/api/cart/items/bulk \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "items": [
      { "productId": 1, "quantity": 2 },
      { "productId": 2, "quantity": 1 },
      { "productId": 5, "quantity": 3 }
    ]
  }'
```

MySQL tables `carts` and `cart_items` are created automatically on first app start.

---

## Warehouse API (admin)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/warehouses` | List warehouses (`?includeInactive=true` for admin) |
| `GET` | `/api/warehouses/{id}` | Get warehouse by id |
| `POST` | `/api/warehouses` | Create warehouse |
| `PUT` | `/api/warehouses/{id}` | Update name/location |
| `DELETE` | `/api/warehouses/{id}` | Soft-deactivate warehouse |

### Example: create warehouse

```bash
curl -X POST http://localhost:8080/api/warehouses \
  -H "Content-Type: application/json" \
  -d '{"name":"East DC","code":"WH-EAST","location":"New York"}'
```

---

## Inventory API (admin)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/inventory` | List stock (`?warehouseId=`, `?productId=`) |
| `GET` | `/api/inventory/{id}` | Get inventory record |
| `GET` | `/api/inventory/warehouse/{warehouseId}` | All stock at a warehouse |
| `GET` | `/api/inventory/product/{productId}` | Stock across warehouses (totals + breakdown) |
| `POST` | `/api/inventory` | Add stock (restock) |
| `PATCH` | `/api/inventory/adjust` | Adjust available quantity by delta |

Each record tracks `quantityAvailable`, `quantityReserved`, and `totalOnHand`.

Checkout reserves stock with **pessimistic locking** — inactive warehouses are excluded from allocation.

### Example: restock and view product stock

```bash
curl -X POST http://localhost:8080/api/inventory \
  -H "Content-Type: application/json" \
  -d '{"warehouseId":1,"productId":1,"quantity":100}'

curl http://localhost:8080/api/inventory/product/1
```

MySQL tables `warehouses` and `inventory_items` are created automatically on first app start.

---

## Checkout API

| Method | Endpoint | Role (intended) | Description |
|--------|----------|-----------------|-------------|
| `POST` | `/api/checkout` | Customer | Atomically reserve stock, charge payment, create order |

### Checkout flow (single transaction)

1. Load active cart
2. Reserve inventory across warehouses (greedy allocation)
3. Calculate subtotal + 10% tax
4. Process payment (stub gateway)
5. Create order (`CREATED` → `CONFIRMED` after payment) + payment record
6. Mark cart `CHECKED_OUT`
7. Publish async audit/notification event (non-blocking)

### Example: full checkout flow

```bash
# 1. Stock the warehouse
curl -X POST http://localhost:8080/api/warehouses \
  -H "Content-Type: application/json" \
  -d '{"name":"East DC","code":"WH-EAST","location":"NYC"}'

curl -X POST http://localhost:8080/api/inventory \
  -H "Content-Type: application/json" \
  -d '{"warehouseId":1,"productId":1,"quantity":50}'

# 2. Add to cart
curl -X POST http://localhost:8080/api/cart/items \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"productId":1,"quantity":2}'

# 3. Checkout
curl -X POST http://localhost:8080/api/checkout \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"paymentMethod":"CARD"}'
```

---

## Orders API

| Method | Endpoint | Role (intended) | Description |
|--------|----------|-----------------|-------------|
| `GET` | `/api/orders?customerId={id}` | Customer | List customer orders |
| `GET` | `/api/orders/{orderId}` | Customer / Staff | Get order details (includes `allowedNextStatuses`) |
| `PATCH` | `/api/orders/{orderId}/status` | Warehouse Staff / Admin | Advance order through fulfillment states |
| `POST` | `/api/orders/{orderId}/cancel` | Customer | Cancel from `CREATED` or `CONFIRMED` only (releases inventory + refund) |
| `POST` | `/api/orders/{orderId}/return` | Customer | Request return from `DELIVERED` (full refund) |

### Order lifecycle

```
CREATED → CONFIRMED → PACKED → SHIPPED → DELIVERED → RETURNED
   ↓          ↓
CANCELLED  CANCELLED
```

- Cancellation is **not** allowed once the order reaches `PACKED`, `SHIPPED`, `DELIVERED`, or `RETURNED`.
- Concurrent cancel vs pack on a `CONFIRMED` order uses optimistic locking (`@Version`); the loser gets HTTP `409 Conflict`.
- Cancel restores reserved stock to `quantityAvailable`.
- Return issues a full refund via the payment service.

### Example: fulfill and return an order

```bash
# After checkout (order id = 1, status CONFIRMED)
curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"PACKED"}'

curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"SHIPPED"}'

curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"DELIVERED"}'

curl -X POST http://localhost:8080/api/orders/1/return \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"reason":"Defective item"}'
```

### Example: cancel before packing

Only allowed while status is `CREATED` or `CONFIRMED`:

```bash
curl -X POST http://localhost:8080/api/orders/1/cancel \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"reason":"Changed mind"}'
```

---

## Payment API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/payments/{paymentId}` | Get payment by id |
| `GET` | `/api/payments/order/{orderId}` | Get payment for an order |
| `GET` | `/api/payments?customerId={id}` | List customer payments |
| `POST` | `/api/payments/{paymentId}/refund` | Process full or partial refund |

**Supported methods at checkout:** `CARD`, `UPI`, `WALLET`, `COD`

**Payment statuses:** `PENDING`, `SUCCESS`, `FAILED`, `PARTIALLY_REFUNDED`, `REFUNDED`

Checkout delegates to `PaymentService` + `StubPaymentGateway`. Failed payments return HTTP `402`.

### Example: view payment after checkout

```bash
curl http://localhost:8080/api/payments/order/1
```

### Example: partial refund

```bash
curl -X POST http://localhost:8080/api/payments/1/refund \
  -H "Content-Type: application/json" \
  -d '{"amount":25.00,"reason":"Customer return"}'
```

---

## API Surface (planned — remaining modules)

| Area | Endpoints (draft) | Role |
|------|-------------------|------|
| Inventory | *(implemented — see Warehouse & Inventory APIs)* | Admin |
| Cart | *(implemented — see Cart API above)* | Customer |
| Checkout | *(implemented — see Checkout API above)* | Customer |
| Orders | *(implemented — see Orders API above)* | Customer / Staff |
| Payments | *(implemented — see Payment API above)* | Customer / Admin |
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
| 2026-06-22 | 3 — Cart | `feat: customer cart with add, update, remove, and persistence` |
| 2026-06-22 | 4 — Checkout | `feat: atomic checkout with inventory reservation, payment, and orders` |
| 2026-06-22 | 5 — Payments | `feat: payment gateway, payment APIs, and refund processing` |
| 2026-06-22 | 6 — Inventory | `feat: warehouse and inventory management with multi-location stock APIs` |
| 2026-06-22 | 7 — Orders | `feat: order state machine with cancel, return, and fulfillment APIs` |
| 2026-06-22 | 8 — Platform | `feat: RBAC, discounts, tax, async audit/fulfillment, and concurrency hardening` |

---

## Out of Scope (per assignment)

- UI / frontend
- Deployment, Docker, CI/CD
- Microservices / distributed systems
- OAuth, SSO, MFA
- Production observability / monitoring
