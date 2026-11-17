ALTER TABLE app.auth_session ADD COLUMN authenticated_at timestamptz;
UPDATE app.auth_session SET authenticated_at=created_at;
ALTER TABLE app.auth_session ALTER COLUMN authenticated_at SET NOT NULL;

CREATE TABLE app.mfa_credential (
    user_id uuid PRIMARY KEY REFERENCES app.app_user(user_id),
    secret_encrypted text,
    activated_at timestamptz,
    last_step bigint NOT NULL DEFAULT -1 CHECK (last_step >= -1),
    pending_secret text,
    pending_expires_at timestamptz,
    enrollment_id uuid,
    pending_session_id uuid REFERENCES app.auth_session(session_id),
    pending_replacement boolean NOT NULL DEFAULT false,
    CHECK ((secret_encrypted IS NULL) = (activated_at IS NULL)),
    CHECK ((pending_secret IS NULL) = (pending_expires_at IS NULL)),
    CHECK ((pending_secret IS NULL) = (enrollment_id IS NULL)),
    CHECK ((pending_secret IS NULL) = (pending_session_id IS NULL))
);
CREATE TABLE app.recovery_code (
    user_id uuid NOT NULL REFERENCES app.app_user(user_id),
    fingerprint char(64) NOT NULL,
    created_at timestamptz NOT NULL,
    consumed_at timestamptz,
    PRIMARY KEY(user_id,fingerprint)
);
GRANT SELECT, INSERT, UPDATE ON app.mfa_credential, app.recovery_code TO maint_app;
