# deploy/docker/bl-center

This directory contains the standard Docker host deployment template for `bl-center`.

## Contents

- `docker-compose.yml`: remote host compose template
- `.env`: deployment variable example
- `.env.local`: local image smoke-test variables

## Suggested target layout

```text
/opt/sybase/bl-center
|- docker-compose.yml
|- .env
`- data/
```

## Key variables

- `IMAGE_REPO`: container registry repository
- `IMAGE_TAG`: image tag to deploy
- `HOST_PORT`: exposed host port
- `SERVER_PORT`: Spring Boot server port in the container
- `SPRING_PROFILES_ACTIVE`: runtime profile
- `BL_CENTER_DATASOURCE_URL` / `BL_CENTER_DATASOURCE_USERNAME` / `BL_CENTER_DATASOURCE_PASSWORD`: DM datasource
- `JAVA_OPTS`: JVM options
- `TZ`: timezone

## Manual deployment example

```bash
cp deploy/docker/bl-center/.env /opt/sybase/bl-center/.env
docker login registry.example.com
docker compose --env-file /opt/sybase/bl-center/.env -f deploy/docker/bl-center/docker-compose.yml pull
docker compose --env-file /opt/sybase/bl-center/.env -f deploy/docker/bl-center/docker-compose.yml up -d
```

## First legacy-DM onboarding

For the first sync of a manually created DM schema into Flyway management, temporarily set these variables in `.env`:

```text
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
SPRING_FLYWAY_BASELINE_VERSION=10
SPRING_FLYWAY_BASELINE_DESCRIPTION=legacy-dm-onboard
```

Then run the normal `docker compose up -d` flow once. After Flyway creates `flyway_schema_history` and applies `V11`, clear the three variables and redeploy so later startups run with ordinary Flyway validation/migration only.

## Local image test

```bash
docker build -f bl-center/Dockerfile -t sybase-bl-center:local .
cd deploy/docker/bl-center
docker compose --env-file .env.local -f docker-compose.yml up -d
```
