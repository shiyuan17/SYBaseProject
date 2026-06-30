# scripts/dev

Local development helper scripts.

## Local Spring Boot launchers

- `run-bl-center-dev.cmd` / `run-bl-center-dev.sh`: start `bl-center` with Maven Wrapper, compile dependent modules, and keep a background hot-reload watcher running for `bl-center` plus the shared `common/*` sources.
- `run-auth-center-dev.cmd` / `run-auth-center-dev.sh`: start `auth-center` with Maven Wrapper, compile dependent modules, and keep a background hot-reload watcher running for `auth-center` plus the shared `common/*` sources.
- `run-centers-dev.cmd` / `run-centers-dev.ps1`: one-click start both `bl-center` and `auth-center` in separate Windows terminal windows with `SPRING_PROFILES_ACTIVE=dev`.
- `run-bl-center-jar.sh` / `run-auth-center-jar.sh`: manage built Spring Boot jar processes with `start`, `stop`, `pause`, `resume`, `restart`, `status`, and `logs`.
- All scripts use the repository-local Maven cache via `-Dmaven.repo.local=.m2/repository` and do not require a globally installed `mvn`.
- The hot-reload watcher recompiles into `target/classes` and updates a devtools restart trigger file so Spring Boot restarts automatically after Java or resource changes.
- Watcher output is appended to `.logs/backend.log`. If a compile fails after a save, check that file first.
- Override datasource or JWT variables in your shell before running the script when you need non-default local settings.
- JAR launcher PID files are written under `tmp/dev-services/` by default; service logs are appended to `.logs/backend.log`.

## IntelliJ hot reload

- `bl-center` and `auth-center` now include `spring-boot-devtools` on the dev runtime classpath.
- Use the shared `BlCenter Dev` / `AuthCenter Dev` Spring Boot run configurations.
- In IntelliJ, enable automatic project build while the app is running so saving files refreshes `target/classes` and triggers a devtools restart.
- If automatic build is disabled, the app still restarts after an explicit `Build Project`.
