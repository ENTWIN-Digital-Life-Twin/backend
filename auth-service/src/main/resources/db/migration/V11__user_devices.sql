CREATE TABLE user_devices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    device_id VARCHAR(64) NOT NULL,
    device_label VARCHAR(255),
    first_seen_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_user_devices_user_device UNIQUE (user_id, device_id)
);

CREATE INDEX idx_user_devices_user_id ON user_devices (user_id);
