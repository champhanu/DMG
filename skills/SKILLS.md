# Skills Used During Development

This folder documents the **Cursor Agent Skills** and AI tooling used while building DMG as a **spec-driven** project.

---

## Spec-Driven Workflow

Development followed a strict spec-first approach:

```
README (spec) → AGENTS.md (agent guide) → implement module → tests → update spec → commit
```

| Step | Who | Action |
|------|-----|--------|
| 1 | User / assignment | Define requirements in README problem statement |
| 2 | AI agent | Read `README.md` + `AGENTS.md` before any code |
| 3 | AI agent | Implement **one module** per iteration |
| 4 | AI agent | Add integration tests as acceptance criteria |
| 5 | AI agent | Update README assumptions + AGENTS decisions log |
| 6 | User | Review, commit, push |

This workflow is documented for assignment reviewers to show **AI-assisted, spec-driven development** — not ad-hoc coding.

---

## Cursor Agent Skills

| Skill | Path | Used For |
|-------|------|----------|
| Create Skill | `~/.cursor/skills-cursor/create-skill/SKILL.md` | Documenting agent workflows |
| Create Rule | `~/.cursor/skills-cursor/create-rule/SKILL.md` | Project conventions (if added) |
| SDK | `~/.cursor/skills-cursor/sdk/SKILL.md` | Reference only — not used in runtime |

---

## AI Tools

| Tool | Role in This Project |
|------|----------------------|
| **Cursor Agent** | Primary coding assistant — module-by-module implementation against README spec |
| **Claude (via Cursor)** | Architecture decisions, README/AGENTS drafting, code review |

---

## Spec Artifacts (source of truth)

| File | Role |
|------|------|
| `README.md` | Master spec — requirements, APIs, assumptions, roadmap |
| `AGENTS.md` | Living agent guide — state, decisions, constraints |
| `CLAUDE.md` | Quick agent entry — links to spec + hard rules |
| `skills/SKILLS.md` | This file — tooling evidence for submission |

---

## Updates

| Date | Change |
|------|--------|
| 2026-06-22 | Initial skills documentation (skeleton step) |
| 2026-06-23 | Added spec-driven workflow section; linked to AGENTS.md |
