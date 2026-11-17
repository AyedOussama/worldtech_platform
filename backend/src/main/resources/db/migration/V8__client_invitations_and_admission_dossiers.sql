-- Lot 04 expands identity through public use cases, never rewrites V1-V7.
CREATE TABLE app.client_invitation (
    invitation_id uuid PRIMARY KEY,
    email_normalized varchar(254) NOT NULL UNIQUE CHECK (email_normalized=lower(email_normalized)),
    email_display varchar(254) NOT NULL,
    invited_by uuid NOT NULL REFERENCES app.app_user(user_id),
    last_sent_by uuid NOT NULL REFERENCES app.app_user(user_id),
    accepted_user_id uuid UNIQUE REFERENCES app.app_user(user_id),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL CHECK (expires_at>created_at),
    accepted_at timestamptz,
    version bigint NOT NULL DEFAULT 0 CHECK (version>=0),
    CHECK ((accepted_at IS NULL)=(accepted_user_id IS NULL))
);
CREATE TABLE app.client_invitation_link (
    fingerprint char(64) PRIMARY KEY,
    invitation_id uuid NOT NULL REFERENCES app.client_invitation(invitation_id),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL CHECK (expires_at>created_at),
    consumed_at timestamptz,
    revoked_at timestamptz
);
CREATE UNIQUE INDEX client_invitation_current_link_idx ON app.client_invitation_link(invitation_id)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;
CREATE INDEX client_invitation_link_expiry_idx ON app.client_invitation_link(expires_at);

CREATE TABLE app.client_onboarding (
    dossier_id uuid PRIMARY KEY,
    user_id uuid NOT NULL UNIQUE REFERENCES app.app_user(user_id),
    invitation_id uuid NOT NULL UNIQUE REFERENCES app.client_invitation(invitation_id),
    state varchar(24) NOT NULL DEFAULT 'DRAFT' CHECK (state IN ('DRAFT','SUBMITTED','NEEDS_INFORMATION','APPROVED','REJECTED')),
    account_type varchar(16) CHECK (account_type IN ('INDIVIDUAL','BUSINESS')),
    first_name varchar(100),
    last_name varchar(100),
    phone varchar(40),
    address varchar(500),
    organization_name varchar(200),
    version bigint NOT NULL DEFAULT 0 CHECK (version>=0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE INDEX client_onboarding_review_idx ON app.client_onboarding(state,created_at,dossier_id);
CREATE TABLE app.client_onboarding_history (
    event_id uuid PRIMARY KEY,
    dossier_id uuid NOT NULL REFERENCES app.client_onboarding(dossier_id),
    revision bigint NOT NULL CHECK (revision>=0),
    actor_user_id uuid NOT NULL REFERENCES app.app_user(user_id),
    action varchar(40) NOT NULL CHECK (action IN ('CREATED','DRAFT_SAVED','SUBMITTED','NEEDS_INFORMATION','APPROVED','REJECTED')),
    reason varchar(1000),
    occurred_at timestamptz NOT NULL,
    -- Snapshot of declared data, not secrets; immutable to runtime.
    account_type varchar(16), first_name varchar(100), last_name varchar(100), phone varchar(40),
    address varchar(500), organization_name varchar(200),
    UNIQUE(dossier_id,revision),
    CHECK (action NOT IN ('NEEDS_INFORMATION','APPROVED','REJECTED') OR (reason IS NOT NULL AND length(trim(reason))>0))
);
CREATE INDEX client_onboarding_history_page_idx ON app.client_onboarding_history(dossier_id,revision);

CREATE TABLE app.technician_onboarding (
    dossier_id uuid PRIMARY KEY,
    user_id uuid NOT NULL UNIQUE REFERENCES app.app_user(user_id),
    state varchar(24) NOT NULL DEFAULT 'DRAFT' CHECK (state IN ('DRAFT','SUBMITTED','NEEDS_INFORMATION','APPROVED','REJECTED')),
    first_name varchar(100), last_name varchar(100), phone varchar(40), base_city varchar(200),
    radius_km integer CHECK (radius_km BETWEEN 10 AND 200),
    declared_skills varchar(80)[] NOT NULL DEFAULT '{}' CHECK (cardinality(declared_skills)<=20),
    version bigint NOT NULL DEFAULT 0 CHECK (version>=0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE INDEX technician_onboarding_review_idx ON app.technician_onboarding(state,created_at,dossier_id);
CREATE TABLE app.technician_onboarding_history (
    event_id uuid PRIMARY KEY,
    dossier_id uuid NOT NULL REFERENCES app.technician_onboarding(dossier_id),
    revision bigint NOT NULL CHECK (revision>=0),
    actor_user_id uuid NOT NULL REFERENCES app.app_user(user_id),
    action varchar(40) NOT NULL CHECK (action IN ('CREATED','DRAFT_SAVED','SUBMITTED','NEEDS_INFORMATION','APPROVED','REJECTED')),
    reason varchar(1000), occurred_at timestamptz NOT NULL,
    first_name varchar(100), last_name varchar(100), phone varchar(40), base_city varchar(200), radius_km integer,
    declared_skills varchar(80)[] NOT NULL,
    UNIQUE(dossier_id,revision),
    CHECK (action NOT IN ('NEEDS_INFORMATION','APPROVED','REJECTED') OR (reason IS NOT NULL AND length(trim(reason))>0))
);
CREATE INDEX technician_onboarding_history_page_idx ON app.technician_onboarding_history(dossier_id,revision);

CREATE TABLE app.command_receipt (
    actor_user_id uuid NOT NULL REFERENCES app.app_user(user_id),
    operation varchar(80) NOT NULL,
    key_digest char(64) NOT NULL,
    request_digest char(64) NOT NULL,
    result_version bigint NOT NULL,
    result_state varchar(24) NOT NULL,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL CHECK (expires_at>created_at),
    PRIMARY KEY(actor_user_id,operation,key_digest)
);
GRANT SELECT,INSERT,UPDATE ON app.client_invitation,app.client_invitation_link,
    app.client_onboarding,app.technician_onboarding TO maint_app;
GRANT SELECT,INSERT ON app.client_onboarding_history,app.technician_onboarding_history,app.command_receipt TO maint_app;
