CREATE TABLE email_verification_codes (
    id               UUID         PRIMARY KEY,
    email            VARCHAR(255) NOT NULL,
    code_hash        VARCHAR(64)  NOT NULL,
    expiration_date  TIMESTAMPTZ  NOT NULL,
    used             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_email_verification_email ON email_verification_codes (email);
CREATE INDEX idx_email_verification_hash ON email_verification_codes (code_hash);
