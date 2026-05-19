# bl-center

`bl-center` is the pathology business module built from the `user-center` skeleton and adapted to real Dameng-backed persistence.

## Structure

- `interfaces`: controller, request/response models, representation assembly
- `application`: use-case orchestration, command/query models, transaction boundary
- `domain`: aggregate, repository abstraction, domain factory and validation rules
- `infrastructure`: MyBatis-Plus persistence, Dameng configuration, observability support

## Current capability

- `POST /api/v1/applications`: create a pathology application
- `GET /api/v1/applications/{id}`: query a pathology application by primary key

## Notes

- `users`-related tables remain in `user-center`
- `bl-center` starts with the `applications` table and can be extended to the remaining pathology tables using the same layering pattern
- `dev` profile connects to Dameng, while `test` profile uses an in-memory repository for repeatable automated tests
