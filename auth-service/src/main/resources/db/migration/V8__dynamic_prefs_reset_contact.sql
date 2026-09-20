ALTER TABLE users
    ADD COLUMN bio VARCHAR(1000);

ALTER TABLE user_preferences
    ADD COLUMN ui_settings TEXT,
    ADD COLUMN assistant_conversations TEXT;

CREATE TABLE password_reset_tokens (
    id               UUID         PRIMARY KEY,
    user_id          UUID         NOT NULL,
    token_hash       VARCHAR(64)  NOT NULL,
    expiration_date  TIMESTAMPTZ  NOT NULL,
    used             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_user ON password_reset_tokens (user_id);

CREATE TABLE contact_messages (
    id          UUID          PRIMARY KEY,
    name        VARCHAR(150)  NOT NULL,
    email       VARCHAR(255)  NOT NULL,
    subject     VARCHAR(200)  NOT NULL,
    message     VARCHAR(4000) NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL
);
