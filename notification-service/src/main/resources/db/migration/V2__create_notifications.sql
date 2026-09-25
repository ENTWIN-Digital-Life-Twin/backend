CREATE TABLE notifications (
    id                   UUID PRIMARY KEY,
    user_id              UUID         NOT NULL,
    notification_type    VARCHAR(40)  NOT NULL,
    title                VARCHAR(255) NOT NULL,
    message              VARCHAR(4000) NOT NULL,
    channel              VARCHAR(20)  NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    scheduled_at         TIMESTAMPTZ  NOT NULL,
    sent_at              TIMESTAMPTZ,
    read_at              TIMESTAMPTZ,
    retry_count          INTEGER      NOT NULL DEFAULT 0,
    reminder_id          UUID,
    source_type          VARCHAR(40),
    source_resource_id   UUID,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    deleted              BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at           TIMESTAMPTZ,

    CONSTRAINT chk_notifications_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_scheduled_at ON notifications (scheduled_at);
CREATE INDEX idx_notifications_read_at ON notifications (read_at);
CREATE INDEX idx_notifications_deleted ON notifications (deleted);
CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, read_at) WHERE deleted = FALSE;
CREATE INDEX idx_notifications_reminder_id ON notifications (reminder_id);
