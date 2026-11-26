-- Phase 1: enforce complete, balanced, single-currency ledger journals.
-- Technical invariant only. Does not seed accounts or define financial posting policy.
-- Safe additive migration; replace the previous posting-only deferred validator.

create or replace function app.check_journal_integrity() returns trigger
language plpgsql
set search_path = ''
as $$
declare
  v_journal_id bigint;
  v_debits numeric;
  v_credits numeric;
  v_lines integer;
  v_currencies integer;
begin
  if tg_table_name = 'journal_entries' then
    v_journal_id := new.id;
  else
    v_journal_id := new.journal_id;
  end if;

  select
    coalesce(sum(p.amount) filter (where p.direction = 'D'), 0),
    coalesce(sum(p.amount) filter (where p.direction = 'C'), 0),
    count(*),
    count(distinct a.currency)
  into v_debits, v_credits, v_lines, v_currencies
  from app.ledger_postings p
  join app.ledger_accounts a on a.id = p.account_id
  where p.journal_id = v_journal_id;

  if v_lines < 2 then
    raise exception 'journal % must contain at least two postings', v_journal_id;
  end if;
  if v_currencies <> 1 then
    raise exception 'journal % must use exactly one currency', v_journal_id;
  end if;
  if v_debits <> v_credits then
    raise exception 'journal % unbalanced: debits %, credits %', v_journal_id, v_debits, v_credits;
  end if;
  return null;
end $$;

drop trigger if exists ledger_postings_balanced on app.ledger_postings;
create constraint trigger ledger_postings_balanced
  after insert on app.ledger_postings
  deferrable initially deferred
  for each row execute function app.check_journal_integrity();

drop trigger if exists journal_entries_balanced on app.journal_entries;
create constraint trigger journal_entries_balanced
  after insert on app.journal_entries
  deferrable initially deferred
  for each row execute function app.check_journal_integrity();

revoke all on function app.check_journal_integrity() from public, anon, authenticated;
