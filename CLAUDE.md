# CLAUDE.md

> **Spec-driven development.** Read the spec first, then code.  
> Full guide: **[AGENTS.md](AGENTS.md)** · Master spec: **[README.md](README.md)**

---

## Quick Context

| Item | Value |
|------|-------|
| **Project** | DMG — E-commerce Order Management System |
| **Repo** | https://github.com/champhanu/DMG |
| **Approach** | **Spec-driven** — README is the source of truth; code follows the spec |
| **Workflow** | One module → tests → update README + AGENTS → commit |

---

## Before Writing Any Code

1. Read **[README.md](README.md)** — problem statement, assumptions, API contracts, roadmap.
2. Read **[AGENTS.md](AGENTS.md)** — current state, decisions log, constraints.
3. Implement **only** the module the user asked for.
4. Add tests that prove the spec requirement.
5. Update README (assumptions, roadmap, dev log) and AGENTS.md (decisions) in the same change.

---

## Spec-Driven Rules (hard constraints)

- **No scope creep** — if it's not in README or explicitly requested, don't build it.
- **Document decisions** — non-obvious choices go in AGENTS.md decisions log.
- **Tests = acceptance criteria** — e.g. concurrent checkout test proves no overselling.
- **One commit per module** — user controls git; you propose the message.

---

## Current State (summary)

All core modules are **done**: catalog, cart, checkout, payments, inventory, orders (state machine), discounts/tax, RBAC, async audit/fulfillment, demo seeder.

Order flow: `CREATED → CONFIRMED → PACKED → SHIPPED → DELIVERED → RETURNED` (+ `CANCELLED` from created/confirmed only).

---

## Key Constraints (from spec)

- No UI, no Docker/CI, no microservices, no OAuth
- REST APIs + MySQL + validation + RBAC + tests
- Inventory: pessimistic lock, no overselling under concurrency
- Checkout: atomic (cart + inventory + payment); downstream async after commit
- RBAC: `admin` / `customer` / `staff` (HTTP Basic)

---

## AI Tooling

See **[skills/SKILLS.md](skills/SKILLS.md)** for Cursor Agent skills used and the documented agent workflow.
