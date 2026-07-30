# TECH_DEBT.md

## Purpose

Track durable backend technical debt discovered during implementation, review, testing, or red-team analysis.

## Entries

| ID | Severity | Source | Impact | Suggested Action | Status |
| --- | --- | --- | --- | --- | --- |
| TD-20260608-001 | High | `RepositoryFileHealthGateTest` during hook validation | Backend `pre-push` fast verification was previously blocked by CRLF and max-line policy violations. | Keep exemptions and file-health policy aligned with actual repository state; reopen if fast verification regresses. | Resolved |
| TD-20260611-001 | Medium | `.\mvnw.cmd -pl bl-center -Dtest=M5SingleApiIntegrationTest test` during M5 archive cabinet delivery | Backend targeted test execution is blocked during `testCompile` by unrelated dirty test/API signature drift in specimen/system-management tests (`JdbcSpecimenWorkflow*`, `ApplicationRegistrationWorkbenchRepository.TechnicalRegistrationDetailSectionOverrides`, `SystemManagementService.CreateUserCommand` / `RecordUserLoginCommand`). Main `bl-center` compile still passes. | Reconcile the dirty specimen/system-management test contracts or run a narrower test-compile profile before relying on full/targeted Maven test execution. | Open |
| TD-20260730-001 | High | Check-item-rule V117 deployment inspection against the current Dameng schema | The deployed schema contains the material-loan Java V115 and permission-seed V116 from `982d269`, while `BL-XC-DEV` contains a different grossing-draft SQL V115 and no V116 artifact. Flyway validation therefore fails before V117; `sync` would invoke `repair` and may rewrite migration history. | Reconcile the divergent V115/V116 source lineages in an approved migration-only change, then validate `inspect -> approved repair if required -> migrate V117 -> inspect` on a Dameng clone before production deployment. Do not repair or sync the current production schema as part of the check-item-rule rollout. | Open |

## Update Rules

- Add a new entry only when the debt is durable and actionable.
- Update `Status` instead of deleting resolved items.
- Reference tests, files, MR, or frontend evidence when applicable.
