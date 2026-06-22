# AGENTS.md — AI-Assisted Development Guide

**Current step:** Inventory & Warehouses complete — next: Fulfillment, Returns, or Auth

---

## Current State

| Item | Status |
|------|--------|
| Catalog / Cart / Checkout / Payments | ✅ |
| Warehouses (full CRUD) | ✅ |
| Inventory (stock, adjust, queries) | ✅ |
| Pessimistic reservation at checkout | ✅ |
| Fulfillment / Auth | ❌ |

---

## Step — Inventory & Warehouses (completed)

**Built:**
- `WarehouseService` — create, update, deactivate, list, get
- `WarehouseController` — `/api/warehouses` REST APIs
- `InventoryService` — stock, adjust, list, warehouse/product views, `reserveProduct` for checkout
- `InventoryController` — `/api/inventory` REST APIs
- `ProductStockResponse` — totals across active warehouses
- `InventoryIntegrationTest`

**Tables:** `warehouses`, `inventory_items` (with `@Version` for optimistic locking on entity)

---

## Decisions Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-06-22 | Split `WarehouseService` from `InventoryService` | Clear separation of warehouse vs stock concerns |
| 2026-06-22 | Soft-deactivate warehouses | Inactive warehouses excluded from checkout allocation |
| 2026-06-22 | `PATCH /api/inventory/adjust` with delta | Supports restock (+) and shrinkage (-) adjustments |
| 2026-06-22 | Pessimistic lock on reserve | Prevents overselling under concurrent checkout |

---

## Next Step

- **Fulfillment:** order status updates by warehouse staff
- **Returns:** release reserved stock + trigger payment refund
