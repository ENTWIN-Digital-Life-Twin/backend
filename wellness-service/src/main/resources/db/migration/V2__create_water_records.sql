CREATE TABLE water_records (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL,
    quantity_ml     INTEGER      NOT NULL,
    consumed_at     TIMESTAMPTZ  NOT NULL,
    beverage_type   VARCHAR(20)  NOT NULL,
    notes           VARCHAR(2000),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT chk_water_quantity_positive CHECK (quantity_ml > 0)
);

CREATE INDEX idx_water_records_user_id ON water_records (user_id);
CREATE INDEX idx_water_records_user_consumed ON water_records (user_id, consumed_at);
CREATE INDEX idx_water_records_not_deleted ON water_records (user_id) WHERE deleted = FALSE;
