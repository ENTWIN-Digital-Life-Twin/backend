ALTER TABLE tasks
    ADD COLUMN subtasks JSONB NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE calendar_events
    ADD COLUMN participants JSONB NOT NULL DEFAULT '[]'::jsonb;
