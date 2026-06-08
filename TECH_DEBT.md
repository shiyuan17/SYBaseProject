# TECH_DEBT.md

## Purpose

Track durable backend technical debt discovered during implementation, review, testing, or red-team analysis.

## Entries

| ID | Severity | Source | Impact | Suggested Action | Status |
| --- | --- | --- | --- | --- | --- |
| TD-20260608-001 | High | `RepositoryFileHealthGateTest` during hook validation | Backend `pre-push` fast verification was previously blocked by CRLF and max-line policy violations. | Keep exemptions and file-health policy aligned with actual repository state; reopen if fast verification regresses. | Resolved |

## Update Rules

- Add a new entry only when the debt is durable and actionable.
- Update `Status` instead of deleting resolved items.
- Reference tests, files, MR, or frontend evidence when applicable.
