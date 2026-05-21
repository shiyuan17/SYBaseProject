# scripts/dev

Local development helper scripts.

## Local Spring Boot launchers

- `run-bl-center-dev.cmd` / `run-bl-center-dev.sh`: start `bl-center` with Maven Wrapper and the `dev` profile defaults used by the shared IntelliJ run configuration.
- `run-auth-center-dev.cmd` / `run-auth-center-dev.sh`: start `auth-center` with Maven Wrapper and the `dev` profile defaults used by the shared IntelliJ run configuration.
- All scripts use the repository-local Maven cache via `-Dmaven.repo.local=.m2/repository` and do not require a globally installed `mvn`.
- Override datasource or JWT variables in your shell before running the script when you need non-default local settings.
