# MR Review

## Summary

- Purpose:
- Impact:
- Validation:
- Risks:

## Dynamic Workflow Router

Select the primary review workflow that matches this task. Red Team Review is required for every high-risk change and must not be replaced by another workflow.

- [ ] API Review: REST contract, request/response model, error code, backward compatibility
- [ ] DB Review: migration, rollback, index/constraint, seed data, data compatibility
- [ ] Security Review: authentication, authorization, patient data, report data, sensitive logs
- [ ] Architecture Review: DDD layer boundary, domain model, repository contract, shared module impact
- [ ] Execution Driven Debug: production issue, log-first diagnosis, reproduction, fix evidence, rollback path
- [ ] Red Team Review: adversarial review for bypasses, data loss, broken assumptions, rollback gaps

## Red Team Review

- [ ] Tried to prove the change can bypass authentication, authorization, tenant, or data-scope rules.
- [ ] Tried to prove patient/report/business data can be leaked, corrupted, duplicated, or lost.
- [ ] Tried to prove DB migration, rollback, or compatibility assumptions can fail.
- [ ] Tried to prove error handling hides failures or makes recovery ambiguous.
- [ ] Tried to prove logs expose secrets, tokens, patient data, or report details.
- [ ] Documented any rejected attack path or remaining residual risk.

## Workflow Evidence

- [ ] Relevant `docs/AGENTS.md` and scoped rules were read.
- [ ] Frontend/backend cross-checks were completed, if the change crosses repos.
- [ ] Required validation commands are listed with real results.
- [ ] Unverified items are explicitly marked with reasons.
- [ ] Red-zone changes have explicit human confirmation.
