CREATE TABLE user_profiles (
                               id UUID PRIMARY KEY,
                               display_name TEXT,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW()
);