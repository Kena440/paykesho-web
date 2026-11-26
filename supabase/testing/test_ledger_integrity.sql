-- Ledger invariant tests. Run against a disposable database after the ledger migration.
begin;

do $$
declare
  a_zmw bigint;
  a_usd bigint;
  j bigint;
  rejected boolean;
  key text := 'ledger-test-' || gen_random_uuid()::text;
begin
  insert into app.ledger_accounts(code,name,type,currency)
    values ('TZ-'||substr(gen_random_uuid()::text,1,16),'Test ZMW','ASSET','ZMW')
    returning id into a_zmw;
  insert into app.ledger_accounts(code,name,type,currency)
    values ('TU-'||substr(gen_random_uuid()::text,1,16),'Test USD','ASSET','USD')
    returning id into a_usd;

  -- A valid balanced journal must pass.
  insert into app.journal_entries(reference,idempotency_key,event_type)
    values ('TEST-REF-'||gen_random_uuid()::text,key,'TEST')
    returning id into j;
  insert into app.ledger_postings(journal_id,account_id,direction,amount)
    values (j,a_zmw,'D',100.00),(j,a_zmw,'C',100.00);
  set constraints all immediate;

  -- Empty journal must be rejected.
  set constraints all deferred;
  rejected := false;
  begin
    insert into app.journal_entries(reference,idempotency_key,event_type)
      values ('TEST-REF-'||gen_random_uuid()::text,key||'-empty','TEST');
    set constraints all immediate;
  exception when others then
    rejected := position('at least two postings' in sqlerrm) > 0;
  end;
  if not rejected then raise exception 'empty journal test failed'; end if;

  -- Unequal debits/credits must be rejected.
  set constraints all deferred;
  rejected := false;
  begin
    insert into app.journal_entries(reference,idempotency_key,event_type)
      values ('TEST-REF-'||gen_random_uuid()::text,key||'-unbalanced','TEST')
      returning id into j;
    insert into app.ledger_postings(journal_id,account_id,direction,amount)
      values (j,a_zmw,'D',100.00),(j,a_zmw,'C',99.00);
    set constraints all immediate;
  exception when others then
    rejected := position('unbalanced' in sqlerrm) > 0;
  end;
  if not rejected then raise exception 'unbalanced journal test failed'; end if;

  -- Mixed currency postings must be rejected even when amounts balance numerically.
  set constraints all deferred;
  rejected := false;
  begin
    insert into app.journal_entries(reference,idempotency_key,event_type)
      values ('TEST-REF-'||gen_random_uuid()::text,key||'-currency','TEST')
      returning id into j;
    insert into app.ledger_postings(journal_id,account_id,direction,amount)
      values (j,a_zmw,'D',100.00),(j,a_usd,'C',100.00);
    set constraints all immediate;
  exception when others then
    rejected := position('exactly one currency' in sqlerrm) > 0;
  end;
  if not rejected then raise exception 'mixed currency journal test failed'; end if;
end $$;

rollback;
