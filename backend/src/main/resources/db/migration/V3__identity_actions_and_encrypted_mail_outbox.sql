CREATE TABLE app.action_token (
    token_id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES app.app_user(user_id),
    fingerprint char(64) NOT NULL UNIQUE,
    purpose varchar(30) NOT NULL CHECK (purpose IN ('EMAIL_VERIFICATION','PASSWORD_RESET')),
    email_normalized varchar(254) NOT NULL,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz,
    CHECK (expires_at > created_at)
);
CREATE INDEX action_token_user_purpose_idx ON app.action_token(user_id,purpose);
CREATE INDEX action_token_expiry_idx ON app.action_token(expires_at);

CREATE TABLE app.mail_outbox (
    message_id uuid PRIMARY KEY,
    recipient varchar(254) NOT NULL,
    subject varchar(200) NOT NULL,
    encrypted_body text,
    state varchar(20) NOT NULL CHECK (state IN ('PENDING','IN_FLIGHT','DELIVERED','FAILED','EXPIRED')),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    available_at timestamptz NOT NULL,
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    lease_id uuid,
    lease_until timestamptz,
    delivered_at timestamptz,
    CHECK (encrypted_body IS NULL OR encrypted_body ~ '^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$')
);
CREATE INDEX mail_outbox_pending_idx ON app.mail_outbox(available_at) WHERE state IN ('PENDING','IN_FLIGHT');
GRANT SELECT, INSERT, UPDATE ON app.action_token, app.mail_outbox TO maint_app;
