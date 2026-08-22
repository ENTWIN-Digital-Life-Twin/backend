CREATE TABLE calendar_events (
    id                UUID          PRIMARY KEY,
    user_id           UUID          NOT NULL,
    title             VARCHAR(200)  NOT NULL,
    description       VARCHAR(2000),
    start_date_time   TIMESTAMPTZ   NOT NULL,
    end_date_time     TIMESTAMPTZ   NOT NULL,
    all_day           BOOLEAN       NOT NULL DEFAULT FALSE,
    event_type        VARCHAR(30)   NOT NULL,
    location_label    VARCHAR(255),
    recurring         BOOLEAN       NOT NULL DEFAULT FALSE,
    recurrence_rule   VARCHAR(255),
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at        TIMESTAMPTZ,
    CONSTRAINT chk_events_end_after_start CHECK (end_date_time > start_date_time)
);

CREATE INDEX idx_calendar_events_user_id ON calendar_events (user_id);
CREATE INDEX idx_calendar_events_user_range ON calendar_events (user_id, start_date_time, end_date_time);
CREATE INDEX idx_calendar_events_not_deleted ON calendar_events (user_id) WHERE deleted = FALSE;
