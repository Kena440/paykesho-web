#!/bin/sh
set -eu

SERVICE="${1:-}"

case "$SERVICE" in
  backoffice) JAR="/app/jars/backoffice.jar" ;;
  client-portal) JAR="/app/jars/client-portal.jar" ;;
  organization-portal) JAR="/app/jars/organization-portal.jar" ;;
  merchant-portal) JAR="/app/jars/merchant-portal.jar" ;;
  payment-service) JAR="/app/jars/payment-service.jar" ;;
  notification-service) JAR="/app/jars/notification-service.jar" ;;
  file-service) JAR="/app/jars/file-service.jar" ;;
  *) echo "Unknown PayKesho service: $SERVICE" >&2; exit 64 ;;
esac

exec java ${JAVA_OPTS} -Dserver.port=8080 -jar "$JAR"
