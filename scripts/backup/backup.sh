#!/usr/bin/env sh
set -eu

TS="$(date +%Y%m%d_%H%M%S)"
OUT_DIR="${BACKUP_DIR:-./backups}"
mkdir -p "$OUT_DIR"

: "${POSTGRES_DB:?POSTGRES_DB required}"
: "${POSTGRES_USER:?POSTGRES_USER required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD required}"
: "${POSTGRES_HOST:=localhost}"
: "${POSTGRES_PORT:=5432}"

export PGPASSWORD="$POSTGRES_PASSWORD"
FILE="$OUT_DIR/payment_gateway_${TS}.dump"
pg_dump -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -F c "$POSTGRES_DB" > "$FILE"

echo "Backup created: $FILE"
