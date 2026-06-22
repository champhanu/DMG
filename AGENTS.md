# AGENTS.md — AI-Assisted Development Guide

**Current step:** Checkout complete — next: Fulfillment, Auth, or Discounts

---

## Current State

| Item | Status |
|------|--------|
| Catalog APIs | ✅ |
| Cart APIs | ✅ |
| Inventory + reservation | ✅ |
| Checkout (atomic) | ✅ |
| Order read APIs | ✅ |
| Async audit/notification on checkout | ✅ |
| Fulfillment status updates | ❌ |
| Auth / RBAC | ❌ |

---

## Step — Checkout (completed)

**Built:**
- `Warehouse`, `InventoryItem` with optimistic `@Version`
- `Order`, `OrderItem`, `Payment` entities
- Pessimistic inventory reservation across warehouses
- `CheckoutService` — single `@Transactional` checkout
- `POST /api/checkout`, `GET /api/orders`
- `POST /api/warehouses`, `POST /api/inventory` (admin stocking)
- `OrderPlacedEvent` + `@Async` listener (audit + notification logs)
- `CheckoutIntegrationTest` (success, insufficient stock, payment rollback)

**Tables:** `warehouses`, `inventory_items`, `orders`, `order_items`, `payments`

---

## Decisions Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-06-22 | Greedy multi-warehouse allocation | Fulfills from highest-stock warehouse first |
| 2026-06-22 | 10% flat tax at checkout | Placeholder until discounts/tax module |
| 2026-06-22 | Payment stub with `simulatePaymentFailure` | Test rollback without external gateway |
| 2026-06-22 | `@TransactionalEventListener(AFTER_COMMIT)` | Non-blocking pipeline without blocking checkout response |

---

## Next Step

- **Fulfillment:** `PATCH /api/orders/{id}/status` for warehouse staff
- **Auth:** secure admin/customer endpoints
- **Discounts:** replace flat tax, promo codes

See [`skills/SKILLS.md`](skills/SKILLS.md).
