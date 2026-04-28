ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS email TEXT;

ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS onboarding_complete BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS theme TEXT NOT NULL DEFAULT 'system';

ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS timezone TEXT NOT NULL DEFAULT 'Europe/Dublin';

ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can read own profile" ON user_profiles;
DROP POLICY IF EXISTS "Users can update own profile" ON user_profiles;

CREATE POLICY "Users can read own profile"
ON user_profiles
FOR SELECT
                         TO authenticated
                         USING (auth.uid() = id);

CREATE POLICY "Users can update own profile"
ON user_profiles
FOR UPDATE
               TO authenticated
               USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);