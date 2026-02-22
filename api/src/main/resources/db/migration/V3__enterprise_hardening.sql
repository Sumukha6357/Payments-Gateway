alter table idempotency_keys add column if not exists request_fingerprint varchar(512) not null default '';
alter table idempotency_keys add column if not exists response_status int not null default 200;

alter table outbox_events add column if not exists published_at timestamptz;
alter table outbox_events add column if not exists next_attempt_at timestamptz;
alter table outbox_events add column if not exists attempt_count int not null default 0;
alter table outbox_events add column if not exists last_error varchar(1024);
update outbox_events set next_attempt_at = created_at where next_attempt_at is null;

create table if not exists webhook_endpoints (
  id uuid primary key,
  url varchar(512) not null unique,
  secret varchar(255) not null,
  active boolean not null default true,
  created_at timestamptz not null
);

create table if not exists webhook_deliveries (
  id uuid primary key,
  event_id uuid not null references outbox_events(id),
  endpoint_id uuid not null references webhook_endpoints(id),
  attempt int not null,
  status varchar(32) not null,
  response_code int,
  error_message varchar(1024),
  created_at timestamptz not null
);

create index if not exists idx_outbox_next_attempt on outbox_events(status, next_attempt_at);
create index if not exists idx_webhook_deliveries_event on webhook_deliveries(event_id);
