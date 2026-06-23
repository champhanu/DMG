# Future Scope — Scaling DMG Beyond the Current Monolith

> **Purpose:** This document describes how DMG could evolve to operate at production scale.  
> None of this is implemented today — it is presented as the **forward-looking architecture** for interviews, demos, and roadmap discussions.

**Current system:** Single Spring Boot monolith · MySQL · synchronous REST · in-process `@Async` events · stub payment gateway.

---

## Design Principles at Scale

| Principle | Today | At scale |
|-----------|-------|----------|
| Read vs write | Same MySQL for everything | Separate read models, caches, search indexes |
| Consistency | Strong (single DB transaction) | Strong where money/stock matter; eventual elsewhere |
| Integration | In-process Spring events | Durable message bus + outbox pattern |
| Search | SQL `LIKE` on products | Dedicated search engine (Solr / Elasticsearch) |
| Observability | Actuator health | Metrics, traces, structured logs, SLO dashboards |

---

## 1. Inventory — Cache-Aside with DB Sync

### Problem

`GET /api/inventory` and `GET /api/inventory/product/{id}` are read-heavy. At scale, every product page and checkout availability check would hammer MySQL and contend with write locks during reservation.

### Proposed approach

```
                    ┌─────────────┐
  GET inventory ──► │ Redis cache │ ──► cache hit (fast)
                    └──────┬──────┘
                           │ miss
                           ▼
                    ┌─────────────┐
                    │   MySQL     │  (source of truth)
                    └─────────────┘
```

- **Pattern:** Cache-aside with TTL + explicit invalidation.
- **Cache key:** `inventory:product:{productId}`, `inventory:warehouse:{warehouseId}`.
- **Invalidation triggers:**
  - Checkout reservation / cancel / return → invalidate affected product + warehouse keys.
  - Admin restock or adjust → invalidate on write success.
  - Optional: CDC (Debezium) from MySQL binlog for guaranteed sync if multiple services write stock.
- **Consistency rule:** Checkout and reservation **always** go to MySQL with pessimistic lock (as today). Cache is for reads only — never the authority for reservation.
- **Stampede protection:** Single-flight / brief lock when populating cache on miss.

### Expected gain

- Sub-millisecond reads for catalog and availability browsing.
- Reduced DB load during traffic spikes (e.g. flash sales).
- Write path unchanged — no overselling risk.

---

## 2. Catalog & Product Search — Solr / Elasticsearch

### Problem

Product listing with `search`, `categoryId`, filters, and sorting does not scale on relational `LIKE` queries across millions of SKUs.

### Proposed approach

```
 Admin CRUD ──► MySQL (source of truth)
       │
       └──► Index sync job / event ──► Solr or Elasticsearch
                                              │
 Customer browse ◄────────────────────────────┘
 GET /api/products?search=...&category=...
```

- **Index fields:** `name`, `sku`, `description`, `categoryId`, `categoryName`, `price`, `active`, `tags`, `inStock` (denormalized aggregate).
- **Sync strategies:**
  - **Near real-time:** Publish `ProductCreated` / `ProductUpdated` events → indexer consumer updates ES/Solr.
  - **Bulk rebuild:** Nightly full reindex from MySQL for drift correction.
- **API change:** Search endpoints read from ES/Solr; `GET /api/products/{id}` can still hit MySQL for authoritative price/stock or use cache-aside.
- **Faceted search:** Category, price range, brand, availability — native in Solr/ES, expensive in SQL.

### Solr vs Elasticsearch

| | Solr | Elasticsearch |
|---|------|-----------------|
| Strengths | Mature faceting, simpler ops for search-only | Ecosystem (ELK), aggregations, vector search later |
| Fit for DMG | Strong fit for catalog browse/filter | Strong fit if unified observability + search platform |

---

## 3. Payments — Event-Driven Architecture

### Problem

Today, payment charge/refund runs inline in checkout and return flows. At scale, payment providers are slow, flaky, and require retries, idempotency keys, and reconciliation — blocking HTTP threads is unacceptable.

### Proposed approach

```
 Checkout API ──► Order CREATED + Payment PENDING
       │
       └──► Outbox table ──► Message bus (Kafka / RabbitMQ)
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
            Payment worker   Fraud scorer    Audit consumer
                    │
                    └──► PSP (Stripe / Adyen) ──► PaymentCompleted event
                                                          │
                              Order CONFIRMED ◄───────────┘
                              (or PaymentFailed → rollback / retry)
```

**Key patterns:**

| Pattern | Use |
|---------|-----|
| **Transactional outbox** | Publish payment commands in same DB transaction as order creation |
| **Idempotency keys** | `orderId` + `paymentAttempt` on every charge/refund |
| **Saga / choreography** | Checkout saga: reserve inventory → initiate payment → confirm order or compensate |
| **Dead-letter queue** | Failed payments after N retries → manual review |
| **Reconciliation job** | Nightly compare PSP settlements vs `payments` table |

**Events (examples):**

- `PaymentInitiated`, `PaymentAuthorized`, `PaymentCaptured`, `PaymentFailed`
- `RefundInitiated`, `RefundCompleted`, `RefundFailed`
- `ChargebackReceived`

**Consumer responsibilities:**

- Update `payments` status and `orders` state machine.
- Trigger notifications (email/SMS) asynchronously.
- Emit audit events to persistent store (extend current `audit_logs`).

---

## 4. Order & Fulfillment — Durable Event Pipeline

### Today

`OrderPlacedEvent` → `@Async` listeners for audit, fulfillment routing, notifications (in-process, lost on crash).

### At scale

- Replace in-process events with **Kafka topics** partitioned by `orderId`.
- **Fulfillment service** (or module) consumes `OrderConfirmed` → creates tasks per warehouse (already modeled as `fulfillment_tasks`).
- **Notification service** consumes same events → email/push templates.
- **Warehouse staff app** subscribes to `FulfillmentTaskCreated` via WebSocket or polling backed by read replica.

```
 OrderConfirmed (Kafka)
      ├──► Fulfillment router
      ├──► Notification service
      ├──► Analytics / data warehouse
      └──► Audit (immutable event log)
```

---

## 5. Checkout & Inventory Writes — Scale Without Breaking Atomicity

| Concern | Future approach |
|---------|-----------------|
| Hot SKU contention | Shard inventory rows by `warehouseId + productId`; consider reservation service |
| Cross-warehouse allocation | Dedicated allocation engine with greedy / cost-based routing |
| Global orders/sec | Horizontal app instances + connection pooling; DB read replicas for queries |
| Exactly-once checkout | Idempotency-Key header on `POST /api/checkout` stored in DB |

**Non-negotiable:** Inventory reservation and payment initiation remain **strongly consistent** in the write path — cache and search indexes are eventually consistent.

---

## 6. Platform & Operations

| Area | Future enhancement |
|------|-------------------|
| **API gateway** | Rate limiting, auth termination, request routing (Kong / AWS API GW) |
| **Service split** (optional) | Catalog, Inventory, Orders, Payments as separate deployables when team/size warrants |
| **Database** | MySQL primary + read replicas; Flyway/Liquibase migrations; partition `orders` by date |
| **Caching layer** | Redis Cluster for sessions, inventory reads, rate limits |
| **Observability** | OpenTelemetry traces across checkout; Prometheus + Grafana SLOs |
| **Security** | OAuth2/OIDC, JWT, MFA for admin; PCI scope isolation for payment service |
| **Multi-region** | Active-passive DR; stock partitioned by region; CDN for static catalog assets |

---

## 7. Suggested Implementation Phases

| Phase | Focus | Depends on |
|-------|-------|------------|
| **Phase 1** | Redis cache for inventory reads + invalidation on write | Current monolith |
| **Phase 2** | Elasticsearch product index + search API cutover | Phase 1 optional |
| **Phase 3** | Outbox + Kafka for payment commands; async PSP integration | Idempotency in checkout |
| **Phase 4** | Durable fulfillment & notification consumers | Phase 3 event bus |
| **Phase 5** | Read replicas, observability, API gateway | Ops maturity |

---

## 8. What Stays the Same

The **domain model and spec** defined in [README.md](README.md) remain valid at scale:

- Order state machine (`CREATED` → … → `DELIVERED` / `RETURNED` / `CANCELLED`)
- No overselling under concurrency
- Atomic checkout semantics (cart + inventory + payment intent)
- RBAC by role
- Returns and refunds linked to order lifecycle

Future work changes **how** these are implemented — not **what** the business requires.

---

## Summary

| Capability | Current | Future at scale |
|------------|---------|-----------------|
| Inventory reads | MySQL every request | **Redis cache**, invalidated on write |
| Product search | SQL `LIKE` | **Solr / Elasticsearch** index |
| Payments | Inline stub gateway | **Event-driven** bus + PSP workers + outbox |
| Downstream work | In-process `@Async` | **Kafka** consumers (fulfillment, notify, audit) |
| Deployment | Single JAR | Horizontally scaled instances + optional service split |

This roadmap demonstrates that DMG’s current monolith is a **correct, spec-driven foundation** — with a clear path to production-grade e-commerce scale.
