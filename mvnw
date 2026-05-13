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

exec java \
  -Dmaven.multiModuleProjectDirectory="$BASE_DIR" \
  -classpath "$WRAPPER_JAR" \
  "$WRAPPER_MAIN" \
  "$@"

