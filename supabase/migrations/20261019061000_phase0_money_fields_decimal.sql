-- Phase 0 (approved by Amukena, 19 Oct 2026): money and rates as exact decimals.
-- Apply in the same release as the code change (Voucher.interestPercent -> BigDecimal),
-- because the back office runs Hibernate with ddl-auto=validate on Cloudflare.
-- All tables are empty today, so no stored amounts need recomputing.

-- Interest rate in percent (column keeps its historical name).
alter table app.vouchers
  alter column interest_amount type numeric(9,4) using round(interest_amount::numeric, 4);

-- Money columns: Kwacha to the ngwee.
alter table app.vouchers
  alter column amount type numeric(18,2),
  alter column available_amount type numeric(18,2),
  alter column repayment_amount type numeric(18,2);
alter table app.client_balance
  alter column available_balance type numeric(18,2),
  alter column current_balance type numeric(18,2),
  alter column held_balance type numeric(18,2),
  alter column last_transaction_amount type numeric(18,2);
alter table app.payments alter column amount type numeric(18,2);
alter table app.transactions alter column amount type numeric(18,2);
alter table app.settlements alter column settlement_amount type numeric(18,2);
alter table app.payroll_deductions alter column amount_deducted type numeric(18,2);
alter table app.repayment_schedules
  alter column amount_due type numeric(18,2),
  alter column amount_paid type numeric(18,2);

-- Rollback (only if the release is reverted):
-- alter table app.vouchers alter column interest_amount type real;
