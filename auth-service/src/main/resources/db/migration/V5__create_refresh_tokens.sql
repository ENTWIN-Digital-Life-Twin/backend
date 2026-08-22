CREATE TABLE refresh_tokens (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL,
    token           VARCHAR(512)    NOT NULL,
    expiration_date TIMESTAMPTZ     NOT NULL,
    revoked         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
