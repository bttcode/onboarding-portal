#!/bin/sh
# Writes /config.json from environment variables, so the same image runs in every environment.
set -eu

cat > /tmp/config.json <<EOF
{
  "issuer": "${KEYCLOAK_ISSUER}",
  "clientId": "${KEYCLOAK_CLIENT_ID}",
  "requireHttps": ${REQUIRE_HTTPS}
}
EOF
