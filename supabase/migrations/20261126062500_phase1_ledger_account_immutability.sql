-- Phase 1: protect account attributes referenced by posted ledger entries.
-- Operational status may be toggled; identity, ownership, classification and currency
-- are immutable after the account is used. Corrections require a new account.
create or replace function app.prevent_posted_account_mutation() returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if exists (select 1 from app.ledger_postings p where p.account_id = old.id) then
    if new.code is distinct from old.code
       or new.name is distinct from old.name
       or new.type is distinct from old.type
       or new.owner_type is distinct from old.owner_type
       or new.owner_id is distinct from old.owner_id
       or new.currency is distinct from old.currency then
      raise exception 'ledger account attributes are immutable after posting; create a new account';
    end if;
  end if;
  return new;
end $$;

drop trigger if exists ledger_accounts_immutable_after_posting on app.ledger_accounts;
create trigger ledger_accounts_immutable_after_posting
  before update on app.ledger_accounts
  for each row execute function app.prevent_posted_account_mutation();

revoke all on function app.prevent_posted_account_mutation() from public, anon, authenticated;
