-- Test-stage payout profile for the PayKesho Demo Shop (code M20261DEMO1).
--
-- Run ONLY after the site is deployed with LENCO_TEST_MODE = "true" (PR: Lenco
-- test mode). In test mode payouts go to the built-in test double and no money
-- moves. The number 260000000000 is deliberately not a real Zambian mobile
-- number, so even outside test mode Lenco would reject it.
--
-- Effect: client payments to the Demo Shop complete end to end instead of
-- stopping with "Merchant payout method is not configured".
--
-- Idempotent. Undo with the statement at the end.

update app.merchants
set payout_method = 'mobile-money',
    payout_operator = 'airtel',
    payout_phone = '260000000000',
    payout_bank_id = null,
    payout_account_number = null
where code = 'M20261DEMO1';

select id, name, code, payout_method, payout_operator, payout_phone from app.merchants where code = 'M20261DEMO1';

-- Undo (before leaving test mode):
-- update app.merchants set payout_method = null, payout_operator = null, payout_phone = null where code = 'M20261DEMO1';
