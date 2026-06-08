# MR Workflow Packet

## Summary

- Purpose:
- Impact:
- Validation:
- Risks:

## Dynamic Workflow

Choose one primary Workflow and add required modifiers. See `docs/rules/DYNAMIC_WORKFLOW_RULES.md`.

- Primary Workflow: `API / DB / Security / Architecture / Production Debug / Workflow-Infra`
- Trigger signals:
- Expert Agent(s):
- Required modifiers: `Security / DB / Red Team / Frontend Cross-check / Migration Verification`

## Dynamic Tests

- Required test commands:
- Actual results:
- Unverified items and reasons:

## Dynamic Simulation

- Request payloads / failure responses:
- Roles / permissions / data scope:
- Old data / new data / rollback state:
- Logs / replay artifacts, if production debug:
- Target environment differences:

## Dynamic Security

- [ ] Not applicable
- [ ] Authentication, authorization, patient data, report data, audit, export, or sensitive-log impact checked
- Evidence:

## Dynamic Database

- [ ] Not applicable
- [ ] Migration, seed data, SQL, index/constraint, compatibility, or rollback impact checked
- Evidence:

## Red Team

- [ ] Tried to prove authentication, authorization, tenant, or data-scope bypass.
- [ ] Tried to prove patient/report/business data can leak, corrupt, duplicate, or disappear.
- [ ] Tried to prove migration, rollback, or compatibility assumptions can fail.
- [ ] Tried to prove logs expose secrets, tokens, patient data, or report details.
- Attack result:
- Residual risk:

## Cross-Repo Evidence

- Backend evidence:
- Frontend evidence:
- Linked PR/MR:

## Memory Update Packet

Required before merge. Update memory files only when the task changes durable context; list skipped files with reasons.

- Updated memory files:
- Not updated memory files and reasons:
- Related memory IDs: `TD-* / BUG-* / DEC-*`
- Cross-repo memory references:
- Residual risk / follow-up owner:
