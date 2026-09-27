DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT con.conname INTO constraint_name
    FROM pg_constraint con
    WHERE con.conrelid = 'user_devices'::regclass
      AND con.contype = 'f'
      AND pg_get_constraintdef(con.oid) ILIKE '%REFERENCES users%';
    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE user_devices DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE user_devices
    ADD CONSTRAINT user_devices_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT con.conname INTO constraint_name
    FROM pg_constraint con
    WHERE con.conrelid = 'user_identities'::regclass
      AND con.contype = 'f'
      AND pg_get_constraintdef(con.oid) ILIKE '%REFERENCES users%';
    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE user_identities DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE user_identities
    ADD CONSTRAINT user_identities_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
