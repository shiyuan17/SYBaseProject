# DECISIONS.md

## Purpose

Track durable backend and cross-repo decisions that future agents must preserve.

## Decisions

| ID | Date | Context | Decision | Rationale | Impact | Revisit When |
| --- | --- | --- | --- | --- | --- | --- |
| DEC-20260608-001 | 2026-06-08 | AI memory layer | Maintain repo-local memory files in both backend and frontend repos; update them at task delivery time on demand. | Keeps durable context close to the code while avoiding noisy hook-enforced updates. | Future AI tasks must check and update memory files before final handoff. | If automatic agent dispatch or external memory sync is introduced. |
| DEC-20260608-002 | 2026-06-08 | Backend hooks | Use native backend hook scripts with PowerShell implementations and Git hook wrappers, not a Node hook manager. | Fits Maven/GitLab backend and avoids adding frontend tooling to the Java repo. | Hook maintenance lives under `scripts/hooks`; Windows PowerShell support is first-class. | If the team adopts a shared hook manager across repos. |

## Update Rules

- Add decisions only when they change future behavior or prevent repeated debate.
- Include options/rationale in the `Context` and `Rationale` columns when concise.
- Do not delete decisions; mark superseded in `Impact` or add a new decision.
