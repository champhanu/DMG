# AGENTS.md — AI-Assisted Development Guide

**Current step:** Order management complete — next: Discounts, Auth, or advanced fulfillment

---

## Current State

| Item | Status |
|------|--------|
| Catalog / Cart / Checkout / Payments | ✅ |
| Warehouses (full CRUD) | ✅ |
| Inventory (stock, adjust, queries) | ✅ |
| Pessimistic reservation at checkout | ✅ |
| Order state machine (cancel, return, status) | ✅ |
| Auth & RBAC | ❌ |

---

## Step — Order Management (completed)

**Built:**
- `OrderStatus` — `CREATED`, `CONFIRMED`, `PACKED`, `DELIVERED`, `RETURNED`, `CANCELLED`
- `OrderStateMachine` — validates allowed transitions
- `OrderService` — status updates, cancel (inventory release), return (full refund)
- `OrderController` — `PATCH /status`, `POST /cancel`, `POST /return`
- `InventoryService.releaseOrderReservations()` — restores stock on cancel
- `Order.statusReason` — stores cancel/return reason
- `OrderIntegrationTest` — lifecycle, cancel inventory release, invalid transition

**Transitions:**
```
CREATED → CONFIRMED → PACKED → DELIVERED → RETURNED
   ↓          ↓          ↓
CANCELLED  CANCELLED  CANCELLED
```

Checkout sets `CREATED` then `CONFIRMED` after successful payment.

---

## Decisions Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-06-22 | Split `WarehouseService` from `InventoryService` | Clear separation of warehouse vs stock concerns |
| 2026-06-22 | Soft-deactivate warehouses | Inactive warehouses excluded from checkout allocation |
| 2026-06-22 | `PATCH /api/inventory/adjust` with delta | Supports restock (+) and shrinkage (-) adjustments |
| 2026-06-22 | Pessimistic lock on reserve | Prevents overselling under concurrent checkout |
| 2026-06-22 | Central `OrderStateMachine` | Single source of truth for transition rules |
| 2026-06-22 | Cancel releases reserved stock | Prevents ghost reservations after cancellation |
| 2026-06-22 | Return auto-refunds full order total | Links order return to payment refund in one operation |

---

## Next Step

- **Discounts & Tax:** replace flat 10% tax, add promo codes
- **Auth & RBAC:** secure order/cancel/return by customer principal and staff roles
