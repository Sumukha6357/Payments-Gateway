alter table ledger_entries
  add constraint chk_ledger_entries_amount_positive check (amount > 0);

create index if not exists idx_wallets_user_id on wallets(user_id);
create index if not exists idx_users_created_at on users(created_at);
create index if not exists idx_transactions_created_at on transactions(created_at);
create index if not exists idx_transactions_idempotency_key on transactions(idempotency_key);
create index if not exists idx_payments_created_at on payments(created_at);
create index if not exists idx_ledger_entries_created_at on ledger_entries(created_at);
create index if not exists idx_audit_logs_created_at on audit_logs(created_at);
create index if not exists idx_idempotency_keys_created_at on idempotency_keys(created_at);
create index if not exists idx_outbox_status_next_attempt on outbox_events(status, next_attempt_at);

create table if not exists wallet_balances (
  wallet_id uuid primary key references wallets(id) on delete cascade,
  balance numeric(19,4) not null default 0,
  updated_at timestamptz not null default now(),
  constraint chk_wallet_balances_non_negative check (balance >= 0)
);

insert into wallet_balances(wallet_id, balance, updated_at)
select w.id,
       coalesce(sum(case when le.type = 'CREDIT' then le.amount else -le.amount end), 0),
       now()
  from wallets w
  left join ledger_entries le on le.wallet_id = w.id
 group by w.id
on conflict (wallet_id) do nothing;

create or replace function fn_apply_ledger_balance() returns trigger as $$
declare
  new_balance numeric(19,4);
begin
  insert into wallet_balances(wallet_id, balance, updated_at)
  values (new.wallet_id, 0, now())
  on conflict (wallet_id) do nothing;

  if new.type = 'DEBIT' then
    update wallet_balances
       set balance = balance - new.amount,
           updated_at = now()
     where wallet_id = new.wallet_id
     returning balance into new_balance;
  else
    update wallet_balances
       set balance = balance + new.amount,
           updated_at = now()
     where wallet_id = new.wallet_id
     returning balance into new_balance;
  end if;

  if new_balance < 0 then
    raise exception 'Negative wallet balance is not allowed for wallet %', new.wallet_id;
  end if;

  return new;
end;
$$ language plpgsql;

drop trigger if exists trg_apply_ledger_balance on ledger_entries;
create trigger trg_apply_ledger_balance
before insert on ledger_entries
for each row execute function fn_apply_ledger_balance();
