# bl-center

`bl-center` is the pathology business module built from the `user-center` skeleton and adapted to real Dameng-backed persistence.

## Structure

- `interfaces`: controller, request/response models, representation assembly
- `application`: use-case orchestration, command/query models, transaction boundary
- `domain`: aggregate, repository abstraction, domain factory and validation rules
- `infrastructure`: MyBatis-Plus persistence, Dameng configuration, observability support

## Current capability

- Clinical submission workflow: applications, specimen collection/registration, fixation, transport, receipt and tracking
- Technical workflow: pending tasks, grossing, dehydration, embedding, slicing, staining, rework and technical tracking
- Master data and system administration: body parts, medical orders, sampling templates/guidelines, system configs, numbering rules, users and roles
- OpenAPI import: start `bl-center`, then import `GET /v3/api-docs` into Apifox, for example `http://localhost:8080/v3/api-docs`

## Notes

- `users`-related tables remain in `user-center`
- `bl-center` starts with the `applications` table and can be extended to the remaining pathology tables using the same layering pattern
- `dev` profile connects to Dameng, while `test` profile uses an in-memory repository for repeatable automated tests
- OpenAPI annotations on controllers, DTOs, VOs and inline records are now the source of truth; the old hand-written YAML contract is deprecated
