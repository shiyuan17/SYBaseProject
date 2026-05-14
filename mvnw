#!/bin/sh
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
WRAPPER_PROPS="$WRAPPER_DIR/maven-wrapper.properties"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"
WRAPPER_MAIN=org.apache.maven.wrapper.MavenWrapperMain

if [ ! -f "$WRAPPER_PROPS" ]; then
  echo "Missing $WRAPPER_PROPS" >&2
  exit 1
fi

download_wrapper_jar() {
  wrapper_url=$(sed -n 's/^wrapperUrl=//p' "$WRAPPER_PROPS")
  if [ -z "$wrapper_url" ]; then
    echo "wrapperUrl is missing in $WRAPPER_PROPS" >&2
    exit 1
  fi

  mkdir -p "$WRAPPER_DIR"

  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$wrapper_url" -o "$WRAPPER_JAR"
    return
  fi

  if command -v wget >/dev/null 2>&1; then
    wget -q "$wrapper_url" -O "$WRAPPER_JAR"
    return
  fi

  echo "curl or wget is required to download the Maven wrapper jar." >&2
  exit 1
}

if [ ! -f "$WRAPPER_JAR" ]; then
  download_wrapper_jar
fi

resolve_java_cmd() {
  if [ -n "${JAVA_HOME:-}" ]; then
    echo "$JAVA_HOME/bin/java"
    return
  fi

  if [ "$(uname -s)" = "Darwin" ] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
    jdk17_home=$(/usr/libexec/java_home -v 17 2>/dev/null || true)
    if [ -n "$jdk17_home" ]; then
      echo "$jdk17_home/bin/java"
      return
    fi
  fi

  if command -v java >/dev/null 2>&1; then
    command -v java
    return
  fi

  echo ""
}

JAVA_CMD=$(resolve_java_cmd)

if [ -z "$JAVA_CMD" ] || [ ! -x "$JAVA_CMD" ]; then
  echo "A JDK 17 installation is required to run ./mvnw." >&2
  echo "Set JAVA_HOME to JDK 17 or install one discoverable by /usr/libexec/java_home -v 17." >&2
  exit 1
fi

JAVA_VERSION_OUTPUT=$("$JAVA_CMD" -version 2>&1 | sed -n '1p')
case "$JAVA_VERSION_OUTPUT" in
  *\"17* ) ;;
  * )
    echo "SY Base Project requires JDK 17 for Maven Wrapper builds." >&2
    echo "Resolved Java: $JAVA_CMD" >&2
    echo "Version line: $JAVA_VERSION_OUTPUT" >&2
    echo "Set JAVA_HOME to a JDK 17 installation and retry." >&2
    exit 1
    ;;
esac

exec "$JAVA_CMD" \
  -Dmaven.multiModuleProjectDirectory="$BASE_DIR" \
  -classpath "$WRAPPER_JAR" \
  "$WRAPPER_MAIN" \
  "$@"
