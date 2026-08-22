CREATE TABLE health_records (
    id                    UUID PRIMARY KEY,
    user_id               UUID         NOT NULL,
    recorded_at           TIMESTAMPTZ  NOT NULL,
    weight_kg             DOUBLE PRECISION,
    resting_heart_rate    INTEGER,
    systolic_pressure     INTEGER,
    diastolic_pressure    INTEGER,
    temperature_celsius   DOUBLE PRECISION,
    step_count            INTEGER,
    notes                 VARCHAR(2000),
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMPTZ,

    CONSTRAINT chk_health_weight_positive CHECK (weight_kg IS NULL OR weight_kg > 0),
    CONSTRAINT chk_health_rhr_range CHECK (resting_heart_rate IS NULL OR (resting_heart_rate BETWEEN 30 AND 250)),
    CONSTRAINT chk_health_systolic_positive CHECK (systolic_pressure IS NULL OR systolic_pressure > 0),
    CONSTRAINT chk_health_diastolic_positive CHECK (diastolic_pressure IS NULL OR diastolic_pressure > 0),
    CONSTRAINT chk_health_temp_range CHECK (temperature_celsius IS NULL OR (temperature_celsius BETWEEN 30 AND 45)),
    CONSTRAINT chk_health_steps_non_negative CHECK (step_count IS NULL OR step_count >= 0)
);

CREATE INDEX idx_health_records_user_id ON health_records (user_id);
CREATE INDEX idx_health_records_user_recorded ON health_records (user_id, recorded_at);
CREATE INDEX idx_health_records_not_deleted ON health_records (user_id) WHERE deleted = FALSE;
