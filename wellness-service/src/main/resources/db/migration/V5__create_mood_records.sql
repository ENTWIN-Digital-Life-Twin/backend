CREATE TABLE mood_records (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL,
    recorded_at     TIMESTAMPTZ  NOT NULL,
    mood_level      INTEGER      NOT NULL,
    stress_level    INTEGER      NOT NULL,
    fatigue_level   INTEGER      NOT NULL,
    notes           VARCHAR(2000),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT chk_mood_level_range CHECK (mood_level BETWEEN 1 AND 10),
    CONSTRAINT chk_stress_level_range CHECK (stress_level BETWEEN 1 AND 10),
    CONSTRAINT chk_fatigue_level_range CHECK (fatigue_level BETWEEN 1 AND 10)
);

CREATE INDEX idx_mood_records_user_id ON mood_records (user_id);
CREATE INDEX idx_mood_records_user_recorded ON mood_records (user_id, recorded_at);
CREATE INDEX idx_mood_records_not_deleted ON mood_records (user_id) WHERE deleted = FALSE;
