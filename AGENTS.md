# AGENTS.md — AI-Assisted Development Guide

**Current step:** Core platform requirements complete (concurrency, atomic checkout, discounts/tax, RBAC, async pipeline)

---

## Current State

| Item | Status |
|------|--------|
| Catalog / Cart / Checkout / Payments / Orders | ✅ |
| Concurrent-safe inventory (pessimistic lock + test) | ✅ |
| Atomic checkout (cart + inventory + payment) | ✅ |
| Async audit + fulfillment routing after commit | ✅ |
| Discounts + configurable tax | ✅ |
| RBAC (ADMIN / CUSTOMER / WAREHOUSE_STAFF) | ✅ |

---

## Seeded Users (HTTP Basic)

| Username | Password | Role | Notes |
|----------|----------|------|-------|
| `admin` | `admin123` | ADMIN | Catalog, warehouses, inventory, discounts |
| `customer` | `customer123` | CUSTOMER | `customerId=1` |
| `staff` | `staff123` | WAREHOUSE_STAFF | Order status updates, fulfillment |

Set `dmg.security.enforce-rbac=false` in tests to keep integration tests simple.

---

## Key Decisions

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-06-22 | Pessimistic lock on reserve **and** release | Prevents race between checkout and cancel/return |
| 2026-06-22 | `@Version` on `Order` entity | Concurrent cancel vs pack fails with HTTP 409 |
| 2026-06-22 | Cancel only from `CREATED`/`CONFIRMED` | No cancellation after packing or delivery |
| 2026-06-22 | Sort cart lines by `productId` before reserve | Reduces deadlock risk across concurrent checkouts |
| 2026-06-22 | `@Async` + `AFTER_COMMIT` event listeners | Checkout response not blocked by audit/fulfillment |
| 2026-06-22 | Persist `audit_logs` + `fulfillment_tasks` | Durable downstream pipeline vs log-only |
| 2026-06-22 | `TaxService` + `dmg.tax.rate` | Configurable tax without code change |
| 2026-06-22 | Cancel/return refund remaining balance | Paid orders fully reversed on cancel or return |
| 2026-06-22 | HTTP Basic RBAC | Assignment scope; no OAuth/SSO |

---

## Next Step

- Customer principal replaces `customerId` request param
- Partial line-item returns
- Email/SMS notification integration (currently logged)
