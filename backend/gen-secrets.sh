#!/bin/bash
# Generates per-deployment credentials once and persists them.
#
# Addresses DNV finding VULN-18127-04-002 (hardcoded credentials): instead of
# shipping fixed defaults in the repo, each deployment gets unique secrets that
# are created on first run and reused afterwards.
#
# The file is gitignored and readable only by its owner. Sourced by start.sh
# and install.sh; safe to run repeatedly (existing values are never changed).
#
# Usage:  . "$(dirname "$0")/gen-secrets.sh"     # sources + creates if needed

# NOTE: no `set -e`/`-u` here on purpose - this file is *sourced* by start.sh
# and install.sh, so changing shell options would silently alter the caller's
# error handling (an unset variable elsewhere would abort the whole startup).

SECRETS_FILE="${SECRETS_FILE:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.secrets.env}"

_rand() { openssl rand -hex 24; }

if [ ! -f "$SECRETS_FILE" ]; then
    umask 077
    cat > "$SECRETS_FILE" <<INNER
# SECUR-EU deployment secrets — generated $(date -u '+%Y-%m-%dT%H:%M:%SZ')
# Unique to this deployment. Never commit this file.
POSTGRES_PASSWORD=$(_rand)
MONGO_PASSWORD=$(_rand)
MSF_DB_PASSWORD=$(_rand)
JWT_SECRET=$(openssl rand -hex 32)
INNER
    chmod 600 "$SECRETS_FILE"
    echo "  Generated new deployment secrets: $SECRETS_FILE"
fi

# Ensure any key added in a later release is backfilled without touching existing ones
for key in POSTGRES_PASSWORD MONGO_PASSWORD MSF_DB_PASSWORD; do
    grep -q "^${key}=" "$SECRETS_FILE" || { echo "${key}=$(_rand)" >> "$SECRETS_FILE"; }
done
grep -q '^JWT_SECRET=' "$SECRETS_FILE" || echo "JWT_SECRET=$(openssl rand -hex 32)" >> "$SECRETS_FILE"

set -a
# shellcheck disable=SC1090
. "$SECRETS_FILE"
set +a
