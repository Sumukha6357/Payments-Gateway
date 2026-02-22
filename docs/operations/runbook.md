# Operations Runbook

## Daily Backup
Use `scripts/backup/backup.sh` with env vars:
- `POSTGRES_HOST`
- `POSTGRES_PORT`
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- optional `BACKUP_DIR`

Example:
```bash
POSTGRES_HOST=localhost POSTGRES_PORT=5432 POSTGRES_DB=payment_gateway POSTGRES_USER=postgres POSTGRES_PASSWORD=secret ./scripts/backup/backup.sh
```

## Restore Validation
Use `scripts/backup/restore.sh` in non-production first.

Example:
```bash
POSTGRES_HOST=localhost POSTGRES_PORT=5432 POSTGRES_DB=payment_gateway_restore POSTGRES_USER=postgres POSTGRES_PASSWORD=secret BACKUP_FILE=./backups/payment_gateway_20260222_020000.dump ./scripts/backup/restore.sh
```

After restore, validate:
1. `select count(*) from flyway_schema_history;`
2. `select count(*) from ledger_entries;`
3. Run API smoke tests.

## Migration Rollback Strategy
Flyway migrations are forward-only. Rollback strategy:
1. Restore last known-good backup.
2. Deploy previous app version.
3. Apply corrective migration after incident review.

## Reconciliation
`LedgerReconciliationService` runs daily at 02:00 UTC by default:
- compares ledger aggregate vs `wallet_balances`
- logs mismatches and emits `ledger_reconciliation_mismatches`
