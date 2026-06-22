# AGENTS.md — AI-Assisted Development Guide

This file documents how AI agents (Cursor, Claude, etc.) are used to build the DMG e-commerce order management system.  
**Keep this file updated after every module.**

---

## Project Context

- **Name:** DMG (E-commerce Order Management)
- **Goal:** Hiring assignment — feature-rich Spring Boot REST API
- **Approach:** Incremental modules, one commit per step, README updated each time
- **Main class:** `Ecommerce.Management.main`
- **Package root:** `Ecommerce.Management`
- **Database:** MySQL locally (`dmg` schema), H2 in-memory for tests

---

## Development Rules for Agents

1. **One module per step** — do not jump ahead; wait for user confirmation before the next module.
2. **Update README.md** after each module: assumptions table, roadmap status, dev log.
3. **Update this file** after each module: what was built, decisions made, next step.
4. **Minimal diffs** — match existing code style; no over-engineering.
5. **Test core flows** — unit + integration tests required for each module before marking done.
6. **Document assumptions** in README when making scoping choices.
7. **No UI, no Docker, no microservices** — per assignment constraints.

---

## Module Build Order

```
Skeleton → Foundation → Persistence → Auth → Catalog → Inventory
→ Cart → Discounts/Tax → Checkout/Orders → Fulfillment → Returns
→ Async Pipeline → Tests (ongoing)
```

---

## Current State

| Item | Status |
|------|--------|
| Spring Boot app boots | ✅ |
| README.md | ✅ Up to date |
| AGENTS.md | ✅ This file |
| MySQL + JPA configured | ✅ `ddl-auto: update` |
| Security (permit-all stub) | ✅ Step 3 will add RBAC |
| Actuator health endpoint | ✅ `/actuator/health` |
| Async enabled | ✅ `@EnableAsync` |
| REST APIs (business) | ❌ Not yet |
| Entities / repositories | ❌ Step 2 |

---

## Decisions Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-06-22 | Incremental module commits | Demonstrates git workflow + clear review for evaluators |
| 2026-06-22 | Package layout: layered (controller/service/repository/domain) | Standard Spring Boot convention, easy to navigate |
| 2026-06-22 | Async via Spring Events + `@Async` | Non-blocking checkout without distributed complexity |
| 2026-06-22 | MySQL for local dev, H2 for tests | User has MySQL installed; H2 keeps `mvn test` fast and password-free |
| 2026-06-22 | Hibernate `ddl-auto: update` (no Flyway yet) | Tables auto-sync as entities are added; migrations can come later |
| 2026-06-22 | `application-local.yml` for DB credentials | Passwords stay out of git |

---

## Next Step (Step 2 — Persistence)

When user confirms, implement core entities and repositories:

- `User`, `Role` (enum: ADMIN, CUSTOMER, WAREHOUSE_STAFF)
- `Category`, `Product`
- `Warehouse`, `InventoryItem`
- `Cart`, `CartItem`
- `Order`, `OrderItem`, `OrderStatus` enum
- `Payment`, `Discount`, `ReturnRequest`
- Base entity (`id`, `createdAt`, `updatedAt`)
- Spring Data JPA repositories

Tables will be created automatically in local MySQL on first run.

---

## Skills Used

See [`skills/SKILLS.md`](skills/SKILLS.md) for the full list of Cursor/agent skills referenced during development.
