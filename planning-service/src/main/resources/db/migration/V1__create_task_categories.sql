CREATE TABLE task_categories (
    id               UUID         PRIMARY KEY,
    user_id          UUID,
    name             VARCHAR(100) NOT NULL,
    description      VARCHAR(500),
    color_code       VARCHAR(20),
    system_category  BOOLEAN      NOT NULL DEFAULT FALSE,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_task_categories_user_id ON task_categories (user_id);
CREATE UNIQUE INDEX uk_task_categories_system_name
    ON task_categories (name)
    WHERE system_category = TRUE;
CREATE UNIQUE INDEX uk_task_categories_user_name
    ON task_categories (user_id, name)
    WHERE system_category = FALSE AND user_id IS NOT NULL;
