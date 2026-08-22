CREATE TABLE user_preferences (
    id                            UUID        PRIMARY KEY,
    user_id                       UUID        NOT NULL,
    daily_water_goal_ml           INTEGER,
    preferred_sleep_time          TIME,
    preferred_wake_time           TIME,
    preferred_workout_time        TIME,
    default_transport_mode        VARCHAR(50),
    notification_enabled          BOOLEAN     NOT NULL,
    email_notification_enabled    BOOLEAN     NOT NULL,
    push_notification_enabled     BOOLEAN     NOT NULL,
    ai_recommendation_enabled     BOOLEAN     NOT NULL,
    traffic_integration_enabled   BOOLEAN     NOT NULL,
    CONSTRAINT uk_user_preferences_user UNIQUE (user_id),
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
