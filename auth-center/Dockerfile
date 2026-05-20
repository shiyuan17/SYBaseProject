FROM maven:3.9.9-eclipse-temurin-17 AS build

ARG APP_VERSION=0.1.0-SNAPSHOT
ARG VCS_REF=unknown
ARG BUILD_DATE=unknown

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY common common
COPY tools tools
COPY auth-center auth-center

RUN chmod +x mvnw && ./mvnw -q -pl auth-center -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy

ARG APP_VERSION=0.1.0-SNAPSHOT
ARG VCS_REF=unknown
ARG BUILD_DATE=unknown

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --create-home --home-dir /app app

LABEL org.opencontainers.image.title="auth-center" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.created="${BUILD_DATE}"

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="" \
    SERVER_PORT=8081

COPY --from=build /workspace/auth-center/target/auth-center-*-exec.jar /app/app.jar

RUN chown -R app:app /app

USER app

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=5 \
  CMD sh -c 'curl --fail --silent "http://127.0.0.1:${SERVER_PORT}/actuator/health" >/dev/null || exit 1'

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Duser.timezone=${TZ} -jar /app/app.jar"]
