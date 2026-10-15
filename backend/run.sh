#!/usr/bin/env bash
# Loads backend/.env (gitignored -- see .env.example) into the process environment, then runs the
# packaged jar. application.yml has no working default for DB_PASSWORD on purpose, so this (or an
# equivalent) is required for local dev -- a real deployment should set these via its own secret
# store instead of this file.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
else
  echo "Warning: backend/.env not found -- copy .env.example to .env and fill in DB_PASSWORD." >&2
fi

exec java -jar target/backend-0.0.1-SNAPSHOT.jar
