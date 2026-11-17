-- Additive migration. V1 and its audit permissions are not rewritten.
CREATE TABLE app.app_user (
    user_id uuid PRIMARY KEY,
    status varchar(20) NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'REJECTED')),
    security_version bigint NOT NULL DEFAULT 0 CHECK (security_version >= 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE TABLE app.user_role (
    user_id uuid NOT NULL REFERENCES app.app_user(user_id),
    role varchar(20) NOT NULL CHECK (role IN ('ADMIN', 'CLIENT', 'TECHNICIAN')),
    PRIMARY KEY (user_id, role)
);
CREATE TABLE app.local_credential (
    user_id uuid PRIMARY KEY REFERENCES app.app_user(user_id),
    email_normalized varchar(254) NOT NULL UNIQUE,
    email_display varchar(254) NOT NULL,
    password_hash varchar(255) NOT NULL,
    email_verified_at timestamptz,
    updated_at timestamptz NOT NULL,
    CONSTRAINT local_email_normalized CHECK (email_normalized = lower(email_normalized))
);
CREATE TABLE app.auth_session (
    session_id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES app.app_user(user_id),
    channel varchar(10) NOT NULL CHECK (channel IN ('WEB', 'MOBILE')),
    security_version bigint NOT NULL,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    mfa_at timestamptz,
    CHECK (expires_at > created_at)
);
CREATE INDEX auth_session_user_idx ON app.auth_session(user_id);
CREATE INDEX auth_session_expiry_idx ON app.auth_session(expires_at);

-- Schema from Spring Session JDBC 4.1.1 schema-postgresql.sql, qualified for app.
CREATE TABLE app.spring_session (
    primary_id char(36) NOT NULL PRIMARY KEY,
    session_id char(36) NOT NULL,
    creation_time bigint NOT NULL,
    last_access_time bigint NOT NULL,
    max_inactive_interval integer NOT NULL,
    expiry_time bigint NOT NULL,
    principal_name varchar(100)
);
CREATE UNIQUE INDEX spring_session_ix1 ON app.spring_session(session_id);
CREATE INDEX spring_session_ix2 ON app.spring_session(expiry_time);
CREATE INDEX spring_session_ix3 ON app.spring_session(principal_name);
CREATE TABLE app.spring_session_attributes (
    session_primary_id char(36) NOT NULL REFERENCES app.spring_session(primary_id) ON DELETE CASCADE,
    attribute_name varchar(200) NOT NULL,
    attribute_bytes bytea NOT NULL,
    PRIMARY KEY (session_primary_id, attribute_name)
);

-- Distributed, atomic fixed windows. Keys contain digests, never raw email or IP.
CREATE TABLE app.auth_rate_bucket (
    bucket_key char(64) NOT NULL,
    window_start timestamptz NOT NULL,
    hits integer NOT NULL CHECK (hits > 0),
    expires_at timestamptz NOT NULL,
    PRIMARY KEY (bucket_key, window_start)
);
CREATE INDEX auth_rate_bucket_expiry_idx ON app.auth_rate_bucket(expires_at);

GRANT SELECT, INSERT, UPDATE ON app.app_user, app.local_credential, app.user_role, app.auth_session TO maint_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON app.spring_session, app.spring_session_attributes, app.auth_rate_bucket TO maint_app;
-- No DDL, implicit default grants, deletes of users, or changes to audit/history grants.
