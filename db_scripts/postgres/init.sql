CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE outbox
(
    id                VARCHAR(36) PRIMARY KEY NOT NULL,
    sender            VARCHAR(120),
    recipient         VARCHAR(120),
    subject           VARCHAR(120),
    type              VARCHAR(120),
    body              TEXT,
    locked_by         VARCHAR(36),
    deduplication_key VARCHAR(120),
    scheduled_after   TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at        TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX outbox_scheduled_at_updated_at_index
    ON outbox (scheduled_after, updated_at);

CREATE UNIQUE INDEX deduplication_key_unique_index
    ON outbox (deduplication_key);