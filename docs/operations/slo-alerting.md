# SLOs and Alerts

## SLOs
- Transfer success rate >= 99.5% (5m and 1h windows)
- Webhook delivery success rate >= 99.0% (1h window)
- Idempotency conflict rate <= 1.0% of write requests
- Outbox backlog size <= 500 pending/processing events

## Alert Thresholds
- `transfer_failure_total` > 20 in 5 minutes => warning
- `webhook_failed_total` > 50 in 5 minutes => critical
- `idempotency_conflict_rate_total` > 100 in 10 minutes => warning
- `outbox_backlog_size` > 500 for 10 minutes => warning
- `outbox_backlog_size` > 2000 for 5 minutes => critical

## Operational Health
- readiness endpoint must be UP (`/actuator/health/readiness`)
- liveness endpoint must be UP (`/actuator/health/liveness`)
