CREATE TABLE app.mobile_access_token (
    fingerprint char(64) PRIMARY KEY,
    session_id uuid NOT NULL REFERENCES app.auth_session(session_id),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    CHECK (expires_at > created_at)
);
CREATE INDEX mobile_access_session_idx ON app.mobile_access_token(session_id);
CREATE INDEX mobile_access_expiry_idx ON app.mobile_access_token(expires_at);
CREATE TABLE app.mobile_refresh_token (
    fingerprint char(64) PRIMARY KEY,
    session_id uuid NOT NULL REFERENCES app.auth_session(session_id),
    parent_fingerprint char(64),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz,
    revoked_at timestamptz,
    UNIQUE(fingerprint, session_id),
    FOREIGN KEY(parent_fingerprint, session_id) REFERENCES app.mobile_refresh_token(fingerprint, session_id),
    CHECK (expires_at > created_at)
);
-- Even an implementation defect cannot create two unconsumed branches for one family.
CREATE UNIQUE INDEX mobile_refresh_current_idx ON app.mobile_refresh_token(session_id)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;
CREATE INDEX mobile_refresh_expiry_idx ON app.mobile_refresh_token(expires_at);
GRANT SELECT, INSERT, UPDATE ON app.mobile_access_token, app.mobile_refresh_token TO maint_app;
