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
Skeleton → Foundation → Catalog → Auth → Inventory → Cart
→ Discounts/Tax → Checkout/Orders → Fulfillment → Returns
→ Async Pipeline → Tests (ongoing)
```

---

## Current State

| Item | Status |
|------|--------|
| Spring Boot app boots | ✅ |
| MySQL + JPA configured | ✅ |
| Catalog APIs | ✅ Categories + Products |
| Global error handling | ✅ `ApiError` + validation errors |
| Security (permit-all stub) | ✅ Step 3 will add RBAC |
| Inventory / Cart / Orders | ❌ Not yet |

---

## Decisions Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-06-22 | Incremental module commits | Demonstrates git workflow + clear review for evaluators |
| 2026-06-22 | MySQL for local dev, H2 for tests | User has MySQL installed; H2 keeps `mvn test` fast |
| 2026-06-22 | Hierarchical categories via `parent_id` | Multi-category catalog with root → child browsing |
| 2026-06-22 | Soft delete (`active=false`) for catalog | Avoid breaking future order line references |
| 2026-06-22 | Unique `slug` (category) and `sku` (product) | Prevent duplicate catalog entries |
| 2026-06-22 | `includeInactive=true` query param for admin views | Customers see active items only by default |

---

## Step 2 — Catalog (completed)

**Built:**
- `Category`, `Product` entities + `BaseEntity`
- `CategoryRepository`, `ProductRepository`
- `CategoryService`, `ProductService`
- `CategoryController`, `ProductController`
- DTOs with Jakarta validation
- `GlobalExceptionHandler` (404, 409, 400)
- `CatalogIntegrationTest`, `ProductServiceTest`

**MySQL tables:** `categories`, `products` (auto-created via Hibernate)

---

## Next Step (Step 3 — Auth & RBAC)

When user confirms:
- `User` entity with role enum (`ADMIN`, `CUSTOMER`, `WAREHOUSE_STAFF`)
- BCrypt password auth (basic, no OAuth)
- Secure catalog write endpoints for `ADMIN`
- Secure catalog read endpoints for `CUSTOMER`
- JWT or HTTP Basic — pick simplest that meets assignment

---

## Skills Used

See [`skills/SKILLS.md`](skills/SKILLS.md) for the full list of Cursor/agent skills referenced during development.
