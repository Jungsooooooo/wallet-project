ALTER TABLE wallet_ledger DROP CONSTRAINT uk_wallet_idempotency;

ALTER TABLE wallet_ledger ADD COLUMN transaction_id VARCHAR(64);

ALTER TABLE wallet_ledger ADD CONSTRAINT uk_wallet_ledger_transaction_id UNIQUE (transaction_id);

CREATE TABLE withdrawal_outcomes (
    transaction_id VARCHAR(64) NOT NULL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    status VARCHAR(32) NOT NULL,
    balance_at_outcome NUMERIC(19, 4),
    ledger_entry_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_withdrawal_outcomes_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT fk_withdrawal_outcomes_ledger FOREIGN KEY (ledger_entry_id) REFERENCES wallet_ledger (id)
);

CREATE INDEX idx_withdrawal_outcomes_wallet_id ON withdrawal_outcomes (wallet_id);
