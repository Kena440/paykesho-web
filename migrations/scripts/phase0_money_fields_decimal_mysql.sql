-- Phase 0 (approved 19 Oct 2026): exact decimal for the voucher interest rate (MySQL).
-- Run before deploying the release where Voucher.interestPercent is BigDecimal.
-- Column keeps its historical name interest_amount; it stores a percentage.
ALTER TABLE vouchers MODIFY COLUMN interest_amount DECIMAL(9,4) NULL;

-- Recompute stored repayment amounts that were calculated with float maths.
UPDATE vouchers
   SET repayment_amount = ROUND(amount + ROUND(amount * interest_amount / 100, 2), 2)
 WHERE amount IS NOT NULL AND interest_amount IS NOT NULL AND repayment_amount IS NOT NULL;

-- Rollback: ALTER TABLE vouchers MODIFY COLUMN interest_amount FLOAT NULL;
