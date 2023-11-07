CREATE TABLE outbox (
    id                VARCHAR(36) NOT NULL,
    sender            VARCHAR(120),
    recipient         VARCHAR(120),
    subject           VARCHAR(120),
    type              VARCHAR(120),
    body              TEXT,
    locked_by         VARCHAR(36),
    deduplication_key VARCHAR(120),
    scheduled_after   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX outbox_scheduled_at_updated_at_index(scheduled_after, updated_at),
    UNIQUE KEY deduplication_key_unique_index(deduplication_key)
);