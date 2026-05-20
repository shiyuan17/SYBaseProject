# deploy/docker/auth-center

This directory contains the standard Docker host deployment template for `auth-center`.

## Contents

- `docker-compose.yml`: remote host compose template
- `.env`: deployment variable example
- `.env.local`: local image smoke-test variables

## Suggested target layout

```text
/opt/sybase/auth-center
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
- `AUTH_CENTER_DATASOURCE_URL` / `AUTH_CENTER_DATASOURCE_USERNAME` / `AUTH_CENTER_DATASOURCE_PASSWORD`: DM datasource
- `SECURITY_AUTH_JWT_SM2_PRIVATE_KEY` / `SECURITY_AUTH_JWT_SM2_PUBLIC_KEY`: required JWT SM2 key pair for `dev` and `prod`
- `JAVA_OPTS`: JVM options
- `TZ`: timezone

## Manual deployment example

```bash
cp deploy/docker/auth-center/.env /opt/sybase/auth-center/.env
docker login registry.example.com
docker compose --env-file /opt/sybase/auth-center/.env -f deploy/docker/auth-center/docker-compose.yml pull
docker compose --env-file /opt/sybase/auth-center/.env -f deploy/docker/auth-center/docker-compose.yml up -d
```

## Local image test

```bash
docker build -f auth-center/Dockerfile -t sybase-auth-center:local .
cd deploy/docker/auth-center
docker compose --env-file .env.local -f docker-compose.yml up -d
```
