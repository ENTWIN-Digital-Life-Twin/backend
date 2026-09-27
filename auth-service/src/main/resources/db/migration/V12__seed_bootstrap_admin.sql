-- Local bootstrap administrator. Idempotent if the email already exists.
-- Password is BCrypt for: Admin123!

INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    password_hash,
    preferred_language,
    timezone,
    account_status,
    email_verified,
    created_at,
    updated_at
)
SELECT
    '33333333-3333-3333-3333-333333333333',
    'Platform',
    'Admin',
    'admin@entwin.local',
    '$2b$10$lVjXUOEwU7zCGh45gjfVlOH.28rWnajzfn90cFMXRw8BcEo.0AcmK',
    'en',
    'UTC',
    'ACTIVE',
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE lower(email) = 'admin@entwin.local'
);

UPDATE users
SET
    password_hash = '$2b$10$lVjXUOEwU7zCGh45gjfVlOH.28rWnajzfn90cFMXRw8BcEo.0AcmK',
    account_status = 'ACTIVE',
    email_verified = TRUE,
    updated_at = NOW()
WHERE lower(email) = 'admin@entwin.local';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE lower(u.email) = 'admin@entwin.local'
  AND r.name IN ('USER', 'ADMIN')
ON CONFLICT DO NOTHING;

INSERT INTO user_preferences (
    id,
    user_id,
    daily_water_goal_ml,
    preferred_sleep_time,
    preferred_wake_time,
    preferred_workout_time,
    default_transport_mode,
    notification_enabled,
    email_notification_enabled,
    push_notification_enabled,
    ai_recommendation_enabled,
    traffic_integration_enabled
)
SELECT
    '44444444-4444-4444-4444-444444444444',
    u.id,
    2000,
    TIME '23:00',
    TIME '07:00',
    TIME '18:00',
    'WALK',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    FALSE
FROM users u
WHERE lower(u.email) = 'admin@entwin.local'
ON CONFLICT (user_id) DO NOTHING;
