# AGENTS.md — Spec-Driven Development Guide

> **For humans and AI agents.** This project is built using **spec-driven development**: requirements live in documented specs first; code and tests follow the spec; every decision is recorded before moving on.

**Repository:** https://github.com/champhanu/DMG  
**Stack:** Java 21 · Spring Boot 4.1 · MySQL (local) · H2 (tests)

---

## What Is Spec-Driven Development Here?

In this repo, the **spec is the source of truth** — not the code. Work proceeds in this order:

```
Requirements (README) → Assumptions & decisions → Implementation → Tests → Spec update → Commit
```

| Artifact | Role | Who reads it |
|----------|------|--------------|
| **[README.md](README.md)** | **Master spec** — problem statement, functional requirements, API surface, assumptions, module roadmap, how to run | Humans, reviewers, AI agents |
| **AGENTS.md** (this file) | **Agent working guide** — current state, decisions log, constraints, what to update after each step | AI agents, contributors |
| **[CLAUDE.md](CLAUDE.md)** | **Quick agent entry point** — links here + hard rules before coding | Claude / Cursor agents |
| **[skills/SKILLS.md](skills/SKILLS.md)** | **AI tooling evidence** — which skills/tools were used and the step-by-step workflow | Assignment reviewers |

### Rules every agent and contributor must follow

1. **Read the spec before coding** — start with `README.md` (requirements + assumptions) and this file (current state).
2. **One module at a time** — do not implement features not listed in the roadmap unless the user explicitly expands scope.
3. **Document before you diverge** — if you change behavior, assumptions, or APIs, update `README.md` and the decisions log below in the same change.
4. **Tests prove the spec** — add integration tests for each requirement (e.g. concurrent inventory, order state machine, RBAC).
5. **Commit per module** — one focused commit per roadmap step; message describes the *why*.

---

## Master Requirements (from spec)

These come directly from the assignment / `README.md` problem statement:

| Requirement | Spec section | Implementation |
|-------------|--------------|----------------|
| Multi-category catalog | README → Catalog API | `controller/catalog`, `service/catalog` |
| Customer cart & checkout | README → Cart / Checkout API | `service/cart`, `service/checkout` |
| Multi-warehouse inventory, no overselling | README assumption #6 | Pessimistic lock + concurrent test |
| Atomic checkout (cart + inventory + payment) | README assumption #5 | Single `@Transactional` checkout |
| Async downstream (audit, fulfillment, notify) | README assumption #17 | `@Async` + `AFTER_COMMIT` events |
| Order lifecycle state machine | README → Orders API | `OrderStateMachine`, `OrderService` |
| Optimistic locking on orders | AGENTS decisions | `@Version` on `Order` |
| Discounts, tax, returns, refunds | README → Discounts / Payment API | `DiscountService`, `TaxService`, refunds |
| RBAC (Admin, Customer, Warehouse Staff) | README assumption #3 | `SecurityConfig`, seeded users |
| No UI, no microservices, no OAuth | README → Out of Scope | Enforced by scope |

---

## Current State

| Module | Status |
|--------|--------|
| Skeleton (README, AGENTS, skills) | ✅ |
| Foundation (JPA, Security, MySQL, health) | ✅ |
| Catalog (categories, products) | ✅ |
| Cart | ✅ |
| Checkout (atomic) | ✅ |
| Payments (gateway, refunds) | ✅ |
| Inventory & Warehouses | ✅ |
| Order management (state machine, cancel, return) | ✅ |
| Discounts & Tax | ✅ |
| Fulfillment routing + audit persistence | ✅ |
| Auth & RBAC (HTTP Basic) | ✅ |
| Demo data seeder | ✅ |
| E2E / integration tests | ✅ Ongoing |

**Order lifecycle (spec):**

```
CREATED → CONFIRMED → PACKED → SHIPPED → DELIVERED → RETURNED
   ↓          ↓
CANCELLED  CANCELLED   (not allowed after PACKED)
```

---

## Seeded Users (HTTP Basic)

| Username | Password | Role | Access |
|----------|----------|------|--------|
| `admin` | `admin123` | ADMIN | Catalog, warehouses, inventory, discounts, demo seed |
| `customer` | `customer123` | CUSTOMER | Cart, checkout, own orders (`customerId=1`) |
| `staff` | `staff123` | WAREHOUSE_STAFF | Order status updates, fulfillment |

Tests use `dmg.security.enforce-rbac=false` in `src/test/resources/application.yml`.

---

## Decisions Log

> Every non-obvious choice is recorded here. If you make a new decision, add a row **before** committing.

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-06-22 | Monolithic Spring Boot (no microservices) | Assignment scope |
| 2026-06-22 | README as living spec; AGENTS as agent guide | Spec-driven development workflow |
| 2026-06-22 | One module per commit + README/AGENTS update | Traceable, reviewable progress |
| 2026-06-22 | Pessimistic lock on reserve **and** release | Prevents overselling and ghost reservations |
| 2026-06-22 | `@Version` on `Order` | Concurrent cancel vs pack → HTTP 409 |
| 2026-06-22 | Cancel only from `CREATED`/`CONFIRMED` | Spec: no cancel after packing |
| 2026-06-22 | `SHIPPED` state between `PACKED` and `DELIVERED` | Full fulfillment demo flow |
| 2026-06-22 | `@Async` + `AFTER_COMMIT` listeners | Non-blocking checkout response |
| 2026-06-22 | `audit_logs` + `fulfillment_tasks` tables | Durable downstream pipeline |
| 2026-06-22 | `TaxService` + `dmg.tax.rate` | Configurable tax without code change |
| 2026-06-22 | HTTP Basic RBAC (no OAuth) | Assignment scope |
| 2026-06-23 | Demo seed via `POST /api/admin/demo/seed` + `scripts/seed-demo-via-apis.sh` | Video demo / local data |

---

## Agent Workflow (per module)

When the user asks for the next module:

1. **Read** `README.md` — confirm requirement is in spec/roadmap.
2. **Read** this file — check current state and decisions.
3. **Implement** only that module (minimal diff, match existing conventions).
4. **Test** — integration tests for the spec’s acceptance criteria.
5. **Update spec** — README assumptions table, API docs, module roadmap status.
6. **Update** this file — decisions log + current state.
7. **User commits** — one commit per module (user controls git).

---

## Key Files for Agents

| Path | Purpose |
|------|---------|
| `README.md` | Master spec |
| `AGENTS.md` | This guide |
| `CLAUDE.md` | Quick agent entry |
| `skills/SKILLS.md` | AI tooling documentation |
| `src/main/java/Ecommerce/Management/service/order/OrderStateMachine.java` | Order transition rules |
| `src/main/java/Ecommerce/Management/config/SecurityConfig.java` | RBAC rules |
| `scripts/seed-demo-via-apis.sh` | Populate demo data for presentations |
| `src/test/java/.../order/OrderEndToEndIntegrationTest.java` | Full lifecycle E2E test |

---

## Out of Scope (do not implement without spec change)

- UI / frontend
- Docker, CI/CD, deployment
- Microservices
- OAuth, SSO, MFA
- Production observability beyond basic actuator

---

## Optional Next Steps (not in current spec)

- Bind `customerId` from authenticated principal instead of request param
- Partial line-item returns
- Real email/SMS notifications (currently logged + audit persisted)
