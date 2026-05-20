#!/usr/bin/env sh
set -eu

require_var() {
  var_name="$1"
  eval "var_value=\${$var_name:-}"
  if [ -z "$var_value" ]; then
    echo "Missing required variable: $var_name" >&2
    exit 1
  fi
}

IMAGE_REPO="${IMAGE_REPO:-${CI_REGISTRY_IMAGE:-}}"
if [ -n "$IMAGE_REPO" ] && [ "${IMAGE_REPO#*/auth-center}" = "$IMAGE_REPO" ]; then
  IMAGE_REPO="${IMAGE_REPO}/auth-center"
fi
IMAGE_TAG="${IMAGE_TAG:-${CI_COMMIT_SHA:-local}}"
APP_VERSION="${APP_VERSION:-${CI_COMMIT_TAG:-${CI_COMMIT_REF_SLUG:-local}}}"
VCS_REF="${VCS_REF:-${CI_COMMIT_SHA:-unknown}}"
BUILD_DATE="${BUILD_DATE:-$(date -u +"%Y-%m-%dT%H:%M:%SZ")}"
EXTRA_IMAGE_TAGS="${EXTRA_IMAGE_TAGS:-}"

require_var IMAGE_REPO
require_var IMAGE_TAG

PRIMARY_IMAGE="${IMAGE_REPO}:${IMAGE_TAG}"

echo "Building image ${PRIMARY_IMAGE}"
docker build \
  --pull \
  --file auth-center/Dockerfile \
  --build-arg APP_VERSION="${APP_VERSION}" \
  --build-arg VCS_REF="${VCS_REF}" \
  --build-arg BUILD_DATE="${BUILD_DATE}" \
  --tag "${PRIMARY_IMAGE}" \
  .

for extra_tag in ${EXTRA_IMAGE_TAGS}; do
  [ -n "${extra_tag}" ] || continue
  echo "Tagging image ${IMAGE_REPO}:${extra_tag}"
  docker tag "${PRIMARY_IMAGE}" "${IMAGE_REPO}:${extra_tag}"
done

echo "Pushing image ${PRIMARY_IMAGE}"
docker push "${PRIMARY_IMAGE}"

for extra_tag in ${EXTRA_IMAGE_TAGS}; do
  [ -n "${extra_tag}" ] || continue
  echo "Pushing image ${IMAGE_REPO}:${extra_tag}"
  docker push "${IMAGE_REPO}:${extra_tag}"
done
