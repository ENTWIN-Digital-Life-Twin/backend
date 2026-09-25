CREATE TABLE reminders (
    id                   UUID PRIMARY KEY,
    user_id              UUID         NOT NULL,
    title                VARCHAR(255) NOT NULL,
    message              VARCHAR(4000),
    reminder_type        VARCHAR(40)  NOT NULL,
    trigger_date_time    TIMESTAMPTZ  NOT NULL,
    recurring            BOOLEAN      NOT NULL DEFAULT FALSE,
    recurrence_type      VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    enabled              BOOLEAN      NOT NULL DEFAULT TRUE,
    source_type          VARCHAR(40),
    source_resource_id   UUID,
    advance_minutes      INTEGER,
    next_trigger_at      TIMESTAMPTZ,
    last_triggered_at    TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    deleted              BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at           TIMESTAMPTZ,

    CONSTRAINT chk_reminders_advance_minutes CHECK (advance_minutes IS NULL OR advance_minutes >= 0)
);

CREATE INDEX idx_reminders_user_id ON reminders (user_id);
CREATE INDEX idx_reminders_next_trigger_at ON reminders (next_trigger_at);
CREATE INDEX idx_reminders_enabled ON reminders (enabled);
CREATE INDEX idx_reminders_deleted ON reminders (deleted);
CREATE INDEX idx_reminders_due ON reminders (enabled, deleted, next_trigger_at);
CREATE INDEX idx_reminders_not_deleted ON reminders (user_id) WHERE deleted = FALSE;
