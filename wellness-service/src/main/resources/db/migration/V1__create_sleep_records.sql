CREATE TABLE sleep_records (
    id                  UUID PRIMARY KEY,
    user_id             UUID         NOT NULL,
    sleep_start         TIMESTAMPTZ  NOT NULL,
    wake_time           TIMESTAMPTZ  NOT NULL,
    duration_minutes    INTEGER      NOT NULL,
    quality_score       INTEGER,
    interruptions       INTEGER,
    notes               VARCHAR(2000),
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,

    CONSTRAINT chk_sleep_wake_after_start CHECK (wake_time > sleep_start),
    CONSTRAINT chk_sleep_duration_positive CHECK (duration_minutes > 0),
    CONSTRAINT chk_sleep_quality_range CHECK (quality_score IS NULL OR (quality_score BETWEEN 1 AND 10)),
    CONSTRAINT chk_sleep_interruptions_non_negative CHECK (interruptions IS NULL OR interruptions >= 0)
);

CREATE INDEX idx_sleep_records_user_id ON sleep_records (user_id);
CREATE INDEX idx_sleep_records_user_wake ON sleep_records (user_id, wake_time);
CREATE INDEX idx_sleep_records_not_deleted ON sleep_records (user_id) WHERE deleted = FALSE;
