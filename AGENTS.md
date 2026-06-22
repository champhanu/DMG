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
| README.md | ✅ Skeleton |
| AGENTS.md | ✅ This file |
| Database | ❌ Not yet |
| Security / RBAC | ❌ Not yet |
| REST APIs | ❌ Not yet |
| Tests (beyond context load) | ❌ Not yet |

---

## Decisions Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-06-22 | Incremental module commits | Demonstrates git workflow + clear review for evaluators |
| 2026-06-22 | Package layout: layered (controller/service/repository/domain) | Standard Spring Boot convention, easy to navigate |
| 2026-06-22 | Async via Spring Events + `@Async` | Non-blocking checkout without distributed complexity |

---

## Next Step (Step 1 — Project Foundation)

When user confirms, add to `pom.xml`:

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- Database driver (PostgreSQL or H2 for dev)
- Testcontainers (integration tests)
- Optional: springdoc-openapi for API docs

Plus: `application.yml`, base config classes, health endpoint.

---

## Skills Used

See [`skills/SKILLS.md`](skills/SKILLS.md) for the full list of Cursor/agent skills referenced during development.
