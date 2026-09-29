CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_users_email_lower_trgm
    ON users USING GIN (lower(email) gin_trgm_ops);
