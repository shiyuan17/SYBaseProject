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
- `JAVA_OPTS`: JVM options
- `TZ`: timezone

## Manual deployment example

```bash
cp deploy/docker/bl-center/.env /opt/sybase/bl-center/.env
docker login registry.example.com
docker compose --env-file /opt/sybase/bl-center/.env -f deploy/docker/bl-center/docker-compose.yml pull
docker compose --env-file /opt/sybase/bl-center/.env -f deploy/docker/bl-center/docker-compose.yml up -d
```

## Local image test

```bash
docker build -f bl-center/Dockerfile -t sybase-bl-center:local .
cd deploy/docker/bl-center
docker compose --env-file .env.local -f docker-compose.yml up -d
```
