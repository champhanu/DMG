# CLAUDE.md

> Alias for agent context. See **[AGENTS.md](AGENTS.md)** for the full development guide.

## Quick Context

- **Project:** DMG — E-commerce Order Management (Spring Boot hiring assignment)
- **Repo:** https://github.com/champhanu/DMG
- **Workflow:** One module per step → commit → push → update README + AGENTS.md
- **Current step:** Inventory & Warehouses complete — next: Fulfillment, Returns, or Auth

## Before Writing Code

1. Read `AGENTS.md` and `README.md` for current scope and assumptions.
2. Build only the module the user asked for — do not skip ahead.
3. Update README assumptions table and module roadmap when done.
4. Add tests for the module's core flows.

## Key Constraints

- No UI, no Docker/CI, no microservices, no OAuth
- REST APIs + DB + RBAC + validation + tests required
- Inventory must not oversell under concurrent checkout
- Checkout must be atomic; downstream work must be async/non-blocking
