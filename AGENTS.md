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

## Current State

| Item | Status |
|------|--------|
| Catalog APIs | ✅ Categories + Products |
| Cart APIs | ✅ Add / update / remove / clear |
| Global error handling | ✅ |
| Security (permit-all stub) | ✅ Auth step will add RBAC |
| Inventory / Checkout / Orders | ❌ Not yet |

---

## Decisions Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-06-22 | MySQL for local dev, H2 for tests | User has MySQL installed; H2 keeps `mvn test` fast |
| 2026-06-22 | Hierarchical categories via `parent_id` | Multi-category catalog browsing |
| 2026-06-22 | Soft delete for catalog | Preserve future order references |
| 2026-06-22 | `customerId` query/body param for cart | Auth not built yet; replaced by principal in Auth step |
| 2026-06-22 | One ACTIVE cart per customer (app-enforced) | Checkout will flip status to CHECKED_OUT |
| 2026-06-22 | Price snapshot on cart lines | Stable subtotal before checkout |
| 2026-06-22 | Merge duplicate product lines on add | Standard cart UX |

---

## Step 3 — Cart (completed)

**Built:**
- `Cart`, `CartItem`, `CartStatus` entities
- `CartRepository`, `CartItemRepository`
- `CartService`, `CartController`
- DTOs with validation
- `CartIntegrationTest`, `CartServiceTest`

**MySQL tables:** `carts`, `cart_items`

---

## Next Step (Auth & RBAC or Inventory)

User may choose next module:
- **Auth:** `User` entity, roles, secure endpoints
- **Inventory:** warehouses, stock, concurrent reservation

---

## Skills Used

See [`skills/SKILLS.md`](skills/SKILLS.md) for the full list of Cursor/agent skills referenced during development.
