TRUNCATE TABLE users RESTART IDENTITY CASCADE;

-- Plain password for all generated users: password
INSERT INTO users (
    idempotency_key,
    email,
    password,
    role_enum,
    is_active,
    is_not_blocked
)
SELECT
    uuidv7(),
    format('k6_user_%s@axgiri.tech', idx),
    '$2a$10$j6051IcKePuTLUzHEJCszOYYe4itamK85SqmPgKi9wBYov1TIC8om',
    'USER',
    true,
    true
FROM generate_series(1, 1500) AS idx;
