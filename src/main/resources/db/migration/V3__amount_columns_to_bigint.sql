ALTER TABLE wallets ALTER COLUMN balance BIGINT NOT NULL;

ALTER TABLE wallet_ledger ALTER COLUMN amount BIGINT NOT NULL;

ALTER TABLE wallet_ledger ALTER COLUMN balance_after BIGINT NOT NULL;

ALTER TABLE withdrawal_outcomes ALTER COLUMN amount BIGINT NOT NULL;

ALTER TABLE withdrawal_outcomes ALTER COLUMN balance_at_outcome BIGINT;
