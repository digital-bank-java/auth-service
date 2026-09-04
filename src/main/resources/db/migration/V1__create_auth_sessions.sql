CREATE TABLE auth_sessions (
    session_id UUID PRIMARY KEY,
    username VARCHAR(320) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL,

    CONSTRAINT chk_auth_sessions_time CHECK (expires_at > created_at),
    CONSTRAINT chk_auth_sessions_status CHECK (status IN ('ACTIVE', 'REVOKED'))
);

CREATE INDEX idx_auth_sessions_username_status
    ON auth_sessions (username, status);
