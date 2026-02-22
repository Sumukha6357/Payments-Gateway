alter table wallets add column version bigint;

create table outbox_events (
  id uuid primary key,
  aggregate_type varchar(64) not null,
  aggregate_id varchar(128) not null,
  event_type varchar(64) not null,
  payload jsonb not null,
  status varchar(16) not null,
  created_at timestamptz not null
);

create index idx_outbox_status_created on outbox_events(status, created_at);
