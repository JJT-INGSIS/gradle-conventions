#!/bin/sh
set -eu

SERVICE_NAME='@SERVICE_NAME@'
AGENT_DIRECTORY="$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)"
LICENSE_PATTERN='^[A-Za-z0-9_-]{20,100}$'
VERSION_PATTERN='^[A-Za-z0-9._-]{1,64}$'
DEFAULT_LICENSE_FILE=/run/secrets/newrelic_license
CONFIGURATION_ERROR=64

fail() {
  echo "start-service: $1" >&2
  exit "$CONFIGURATION_ERROR"
}

[ "$#" -eq 1 ] || fail "usage: start-service.sh <application jar>"
APPLICATION_JAR="$1"

case "${NEW_RELIC_ENABLED:-false}" in
  false) exec java -jar "$APPLICATION_JAR" ;;
  true) ;;
  *) fail "NEW_RELIC_ENABLED must be true or false" ;;
esac

ENVIRONMENT="${NEW_RELIC_ENVIRONMENT:-}"
case "$ENVIRONMENT" in
  local | dev | prod) ;;
  *) fail "NEW_RELIC_ENVIRONMENT must be local, dev or prod" ;;
esac

VERSION="${NEW_RELIC_SERVICE_VERSION:-unknown}"
printf '%s' "$VERSION" | grep -Eq "$VERSION_PATTERN" || fail "NEW_RELIC_SERVICE_VERSION has unexpected characters"

LICENSE_FILE="${NEW_RELIC_LICENSE_FILE:-$DEFAULT_LICENSE_FILE}"
[ -r "$LICENSE_FILE" ] || fail "the New Relic license file is missing or unreadable"
LICENSE="$(tr -d '\r\n' < "$LICENSE_FILE")"
printf '%s' "$LICENSE" | grep -Eq "$LICENSE_PATTERN" || fail "the New Relic license file has an unexpected format"

export NEW_RELIC_LICENSE_KEY="$LICENSE"
export NEW_RELIC_APP_NAME="$SERVICE_NAME ($ENVIRONMENT)"
export NEW_RELIC_LABELS="environment:$ENVIRONMENT;service:$SERVICE_NAME;version:$VERSION"
unset LICENSE

exec java "-javaagent:$AGENT_DIRECTORY/newrelic.jar" "-Dnewrelic.config.file=$AGENT_DIRECTORY/newrelic.yml" \
  -jar "$APPLICATION_JAR"
