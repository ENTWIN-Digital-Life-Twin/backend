CREATE TABLE wellness_goals (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL,
    goal_type       VARCHAR(30)  NOT NULL,
    target_value    DOUBLE PRECISION NOT NULL,
    current_value   DOUBLE PRECISION NOT NULL DEFAULT 0,
    unit            VARCHAR(50)  NOT NULL,
    start_date      DATE         NOT NULL,
    target_date     DATE,
    status          VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    completed_at    TIMESTAMPTZ,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT chk_goal_target_positive CHECK (target_value > 0),
    CONSTRAINT chk_goal_current_non_negative CHECK (current_value >= 0),
    CONSTRAINT chk_goal_target_date CHECK (target_date IS NULL OR target_date >= start_date)
);

CREATE INDEX idx_wellness_goals_user_id ON wellness_goals (user_id);
CREATE INDEX idx_wellness_goals_user_status ON wellness_goals (user_id, status);
CREATE INDEX idx_wellness_goals_not_deleted ON wellness_goals (user_id) WHERE deleted = FALSE;
