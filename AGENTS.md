# AGENTS.md — AI-Assisted Development Guide

**Current step:** Payments complete — next: Fulfillment, Returns, or Auth

---

## Current State

| Item | Status |
|------|--------|
| Catalog / Cart / Checkout | ✅ |
| Inventory reservation | ✅ |
| Payment gateway + APIs | ✅ |
| Refunds (partial/full) | ✅ |
| Fulfillment status updates | ❌ |
| Auth / RBAC | ❌ |

---

## Step — Payments (completed)

**Built:**
- `PaymentMethod` enum: CARD, UPI, WALLET, COD
- `PaymentStatus`: PENDING, SUCCESS, FAILED, PARTIALLY_REFUNDED, REFUNDED
- `PaymentGateway` interface + `StubPaymentGateway`
- `PaymentService` — charge at checkout, lookup, refund
- `PaymentController` — `/api/payments` REST APIs
- `PaymentProcessedEvent` + async audit listener
- `PaymentFailedException` → HTTP 402
- Checkout refactored to delegate payment to `PaymentService`

---

## Decisions Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-06-22 | `PaymentGateway` interface | Easy to swap stub for Stripe/Razorpay later |
| 2026-06-22 | Refund API on payment id | Decoupled from returns module; returns can call this later |
| 2026-06-22 | 402 for payment failures | Distinguishes payment decline from generic 400 errors |

---

## Next Step

- **Fulfillment:** order status transitions by warehouse staff
- **Returns:** link return requests to `POST /api/payments/{id}/refund`
