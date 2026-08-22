CREATE TABLE tasks (
    id                        UUID          PRIMARY KEY,
    user_id                   UUID          NOT NULL,
    title                     VARCHAR(200)  NOT NULL,
    description               VARCHAR(2000),
    category_id               UUID,
    priority                  VARCHAR(20)   NOT NULL,
    status                    VARCHAR(30)   NOT NULL,
    planned_duration_minutes  INTEGER       NOT NULL,
    actual_duration_minutes   INTEGER,
    start_date_time           TIMESTAMPTZ,
    deadline                  TIMESTAMPTZ,
    completion_percentage     INTEGER       NOT NULL DEFAULT 0,
    energy_required           VARCHAR(20),
    complexity_level          VARCHAR(20),
    created_at                TIMESTAMPTZ   NOT NULL,
    updated_at                TIMESTAMPTZ   NOT NULL,
    completed_at              TIMESTAMPTZ,
    deleted                   BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at                TIMESTAMPTZ,
    CONSTRAINT fk_tasks_category FOREIGN KEY (category_id) REFERENCES task_categories (id),
    CONSTRAINT chk_tasks_planned_duration CHECK (planned_duration_minutes > 0),
    CONSTRAINT chk_tasks_completion CHECK (completion_percentage BETWEEN 0 AND 100)
);

CREATE INDEX idx_tasks_user_id ON tasks (user_id);
CREATE INDEX idx_tasks_user_status ON tasks (user_id, status);
CREATE INDEX idx_tasks_user_start ON tasks (user_id, start_date_time);
CREATE INDEX idx_tasks_user_deadline ON tasks (user_id, deadline);
CREATE INDEX idx_tasks_category_id ON tasks (category_id);
CREATE INDEX idx_tasks_not_deleted ON tasks (user_id) WHERE deleted = FALSE;
