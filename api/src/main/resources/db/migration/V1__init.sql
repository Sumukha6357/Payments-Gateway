create table users (
  id uuid primary key,
  name varchar(255) not null,
  email varchar(255) not null unique,
  status varchar(32) not null,
  created_at timestamptz not null
);

create table wallets (
  id uuid primary key,
  user_id uuid not null references users(id),
  currency varchar(3) not null,
  status varchar(32) not null,
  created_at timestamptz not null
);

create table transactions (
  id uuid primary key,
  from_wallet_id uuid references wallets(id),
  to_wallet_id uuid references wallets(id),
  amount numeric(19,4) not null,
  status varchar(32) not null,
  idempotency_key varchar(255) unique,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table ledger_entries (
  id uuid primary key,
  wallet_id uuid not null references wallets(id),
  type varchar(16) not null,
  amount numeric(19,4) not null,
  reference_id uuid not null,
  created_at timestamptz not null
);

create table payments (
  id uuid primary key,
  transaction_id uuid not null references transactions(id),
  external_reference varchar(255) unique,
  status varchar(32) not null,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table audit_logs (
  id uuid primary key,
  entity_type varchar(64) not null,
  entity_id varchar(128) not null,
  action varchar(64) not null,
  payload jsonb not null,
  created_at timestamptz not null
);

create table idempotency_keys (
  id uuid primary key,
  idem_key varchar(255) not null unique,
  response_payload jsonb,
  status varchar(32) not null,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create index idx_ledger_wallet on ledger_entries(wallet_id);
create index idx_transactions_status on transactions(status);
