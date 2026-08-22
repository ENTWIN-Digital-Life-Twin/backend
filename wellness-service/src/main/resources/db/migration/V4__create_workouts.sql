CREATE TABLE workouts (
    id                    UUID PRIMARY KEY,
    user_id               UUID         NOT NULL,
    activity_type         VARCHAR(30)  NOT NULL,
    started_at            TIMESTAMPTZ  NOT NULL,
    duration_minutes      INTEGER      NOT NULL,
    intensity             VARCHAR(20)  NOT NULL,
    calories_burned       DOUBLE PRECISION,
    average_heart_rate    INTEGER,
    distance_km           DOUBLE PRECISION,
    completed             BOOLEAN      NOT NULL DEFAULT TRUE,
    notes                 VARCHAR(2000),
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMPTZ,

    CONSTRAINT chk_workout_duration_positive CHECK (duration_minutes > 0),
    CONSTRAINT chk_workout_calories_non_negative CHECK (calories_burned IS NULL OR calories_burned >= 0),
    CONSTRAINT chk_workout_hr_range CHECK (average_heart_rate IS NULL OR (average_heart_rate BETWEEN 30 AND 250)),
    CONSTRAINT chk_workout_distance_non_negative CHECK (distance_km IS NULL OR distance_km >= 0)
);

CREATE INDEX idx_workouts_user_id ON workouts (user_id);
CREATE INDEX idx_workouts_user_started ON workouts (user_id, started_at);
CREATE INDEX idx_workouts_not_deleted ON workouts (user_id) WHERE deleted = FALSE;
