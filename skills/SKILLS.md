# Skills Used During Development

This folder documents the Cursor Agent Skills and AI tooling referenced while building DMG.

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
| **Cursor Agent** | Primary coding assistant — module-by-module implementation |
| **Claude (via Cursor)** | Architecture decisions, README/AGENTS drafting, code review |

---

## Development Workflow (Agent-Driven)

1. User shares assignment requirements or next module name.
2. Agent reads `AGENTS.md` + `README.md` for current state.
3. Agent implements one module with tests.
4. Agent updates README (assumptions, roadmap, dev log) and AGENTS.md (decisions log).
5. User reviews → commit → push → next module.

---

## Updates

| Date | Change |
|------|--------|
| 2026-06-22 | Initial skills documentation (skeleton step) |
