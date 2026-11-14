CREATE TABLE IF NOT EXISTS wallets (
    wallet_id VARCHAR(100) PRIMARY KEY,
    balance NUMERIC(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS transfers (
    transfer_id UUID PRIMARY KEY,
    idempotency_key VARCHAR(200) NOT NULL UNIQUE,
    from_wallet_id VARCHAR(100) NOT NULL,
    to_wallet_id VARCHAR(100) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    wallet_id VARCHAR(100) NOT NULL,
    transfer_id UUID NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_ledger_transfer_wallet_type
        UNIQUE (transfer_id, wallet_id, entry_type)
);

CREATE INDEX IF NOT EXISTS idx_transfers_from_wallet_created
    ON transfers(from_wallet_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transfers_to_wallet_created
    ON transfers(to_wallet_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ledger_wallet_created
    ON ledger_entries(wallet_id, created_at DESC);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_transfers_from_wallet') THEN
        ALTER TABLE transfers ADD CONSTRAINT fk_transfers_from_wallet
            FOREIGN KEY (from_wallet_id) REFERENCES wallets(wallet_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_transfers_to_wallet') THEN
        ALTER TABLE transfers ADD CONSTRAINT fk_transfers_to_wallet
            FOREIGN KEY (to_wallet_id) REFERENCES wallets(wallet_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ledger_entries_wallet') THEN
        ALTER TABLE ledger_entries ADD CONSTRAINT fk_ledger_entries_wallet
            FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ledger_entries_transfer') THEN
        ALTER TABLE ledger_entries ADD CONSTRAINT fk_ledger_entries_transfer
            FOREIGN KEY (transfer_id) REFERENCES transfers(transfer_id);
    END IF;
END $$;
