-- 1. The Wallet Table (Holds the current balance)
CREATE TABLE IF NOT EXISTS wallets (
                                       id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'MYR',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 0, -- Used for Optimistic Locking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 2. Transaction Requests (For Idempotency - Prevents double charging)
CREATE TABLE IF NOT EXISTS transaction_requests (
                                                    id VARCHAR(36) PRIMARY KEY,
    request_id VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 3. The Immutable Ledger (Double-Entry Bookkeeping)
CREATE TABLE IF NOT EXISTS journal_entries (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL REFERENCES transaction_requests(id),
    wallet_id VARCHAR(36) NOT NULL REFERENCES wallets(id),
    type VARCHAR(10) NOT NULL, -- CREDIT or DEBIT
    amount DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 4. Transactional Outbox (Safely queues Kafka events)
CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL, -- We will store the JSON string here
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );