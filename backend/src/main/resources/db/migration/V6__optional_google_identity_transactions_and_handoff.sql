CREATE TABLE app.external_identity (
    identity_id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES app.app_user(user_id),
    provider varchar(20) NOT NULL CHECK (provider='google'),
    issuer varchar(255) NOT NULL CHECK (issuer='https://accounts.google.com'),
    subject varchar(255) NOT NULL,
    created_at timestamptz NOT NULL,
    UNIQUE(provider,issuer,subject),
    UNIQUE(user_id,provider)
);
CREATE TABLE app.google_oauth_transaction (
    transaction_id uuid PRIMARY KEY,
    channel varchar(10) NOT NULL CHECK (channel IN ('WEB','MOBILE')),
    intention varchar(10) NOT NULL CHECK (intention IN ('LOGIN','LINK')),
    user_id uuid REFERENCES app.app_user(user_id),
    source_session_id uuid REFERENCES app.auth_session(session_id),
    security_version bigint,
    browser_fingerprint char(64),
    launch_fingerprint char(64) NOT NULL UNIQUE,
    state_fingerprint char(64) UNIQUE,
    authorization_encrypted text,
    pkce_challenge char(43),
    destination_id varchar(40),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    launched_at timestamptz,
    consumed_at timestamptz,
    completed_at timestamptz,
    CHECK (expires_at>created_at),
    CHECK ((intention='LINK') = (user_id IS NOT NULL AND source_session_id IS NOT NULL AND security_version IS NOT NULL)),
    CHECK ((channel='MOBILE') = (pkce_challenge IS NOT NULL AND destination_id IS NOT NULL))
);
CREATE INDEX google_transaction_expiry_idx ON app.google_oauth_transaction(expires_at);
CREATE TABLE app.mobile_handoff (
    fingerprint char(64) PRIMARY KEY,
    transaction_id uuid NOT NULL UNIQUE REFERENCES app.google_oauth_transaction(transaction_id),
    issuer varchar(255) NOT NULL CHECK (issuer='https://accounts.google.com'),
    subject varchar(255) NOT NULL,
    user_id uuid REFERENCES app.app_user(user_id),
    security_version bigint,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz,
    CHECK (expires_at>created_at AND expires_at<=created_at+interval '60 seconds')
);
CREATE INDEX mobile_handoff_expiry_idx ON app.mobile_handoff(expires_at);
GRANT SELECT, INSERT, DELETE ON app.external_identity TO maint_app;
GRANT SELECT, INSERT, UPDATE ON app.google_oauth_transaction, app.mobile_handoff TO maint_app;
